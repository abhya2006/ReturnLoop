package com.returnloop.identity.auth;

import com.returnloop.common.error.ApiException;
import com.returnloop.common.error.ErrorCode;
import com.returnloop.identity.user.UserAccount;
import com.returnloop.identity.user.UserAccountRepository;
import com.returnloop.organization.Organization;
import com.returnloop.organization.OrganizationRepository;
import com.returnloop.security.JwtService;
import com.returnloop.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentCaptor.forClass;
import org.springframework.mail.SimpleMailMessage;

class AuthServiceTest {

    private UserAccountRepository repository;
    private PasswordEncoder encoder;
    private AuthService service;
    private GoogleTokenVerifier googleTokenVerifier;
    private OrganizationRepository organizationRepository;

    @BeforeEach
    void setUp() {
        repository = mock(UserAccountRepository.class);
        encoder = new BCryptPasswordEncoder(4);
        JwtService jwtService = new JwtService(
                "test-secret-value-that-is-long-enough-for-hmac-sha-256", Duration.ofHours(1));
        JavaMailSender mailSender = mock(JavaMailSender.class);
        organizationRepository = mock(OrganizationRepository.class);
        googleTokenVerifier = mock(GoogleTokenVerifier.class);
        service = new AuthService(repository, encoder, jwtService, organizationRepository, mailSender,
                googleTokenVerifier, "no-reply@returnloop.example");
    }

    private UserAccount activeUser() {
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        user.setOrganizationId(UUID.randomUUID());
        user.setEmail("ops@northwind-retail.example");
        user.setFullName("Amara Okafor");
        user.setRole(Role.ADMIN);
        user.setActive(true);
        user.setPasswordHash(encoder.encode("ReturnLoop!2026"));
        return user;
    }

    @Test
    @DisplayName("valid credentials issue a token and the user's profile")
    void loginSucceeds() {
        UserAccount user = activeUser();
        when(repository.findByEmailIgnoreCase("ops@northwind-retail.example")).thenReturn(Optional.of(user));

        AuthService.LoginResponse response = service.login(
                new AuthService.LoginRequest("ops@northwind-retail.example", "ReturnLoop!2026"));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isPositive();
        assertThat(response.user().email()).isEqualTo("ops@northwind-retail.example");
        assertThat(response.user().role()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("a wrong password is rejected without revealing which field was wrong")
    void wrongPasswordIsRejected() {
        UserAccount user = activeUser();
        when(repository.findByEmailIgnoreCase(any())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(
                new AuthService.LoginRequest("ops@northwind-retail.example", "wrong")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    @DisplayName("an unknown email is rejected with the same error as a wrong password")
    void unknownEmailIsRejected() {
        when(repository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(
                new AuthService.LoginRequest("nobody@example.com", "ReturnLoop!2026")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    @DisplayName("a deactivated account cannot sign in")
    void deactivatedAccountIsRejected() {
        UserAccount user = activeUser();
        user.setActive(false);
        when(repository.findByEmailIgnoreCase(any())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(
                new AuthService.LoginRequest("ops@northwind-retail.example", "ReturnLoop!2026")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("deactivated");
    }

    @Test
    @DisplayName("password reset sends a one-time code by email and does not return it in the API response")
    void passwordResetCodeIsEmailed() {
        UserAccount user = activeUser();
        when(repository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        JavaMailSender mailSender = mock(JavaMailSender.class);
        AuthService resetService = new AuthService(repository, encoder,
                new JwtService("test-secret-value-that-is-long-enough-for-hmac-sha-256", Duration.ofHours(1)),
                mock(com.returnloop.organization.OrganizationRepository.class), mailSender,
                mock(GoogleTokenVerifier.class), "no-reply@returnloop.example");

        AuthService.ForgotPasswordResponse response = resetService.requestPasswordReset(user.getEmail());

        var message = forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());
        assertThat(message.getValue().getTo()).containsExactly(user.getEmail());
        assertThat(message.getValue().getText()).containsPattern("\\b\\d{6}\\b");
        assertThat(response.message()).contains("reset code has been sent");
        String code = message.getValue().getText().replaceAll("\\D", "").substring(0, 6);
        assertThatThrownBy(() -> resetService.resetPassword(user.getEmail(), code, "NewSecurePass!26"))
                .isInstanceOf(ApiException.class);
        resetService.verifyPasswordResetCode(user.getEmail(), code);
        resetService.resetPassword(user.getEmail(), code, "NewSecurePass!26");
        assertThat(encoder.matches("NewSecurePass!26", user.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("first Google sign-in creates a workspace and issues an application session")
    void firstGoogleSignInCreatesWorkspace() {
        when(googleTokenVerifier.verify("verified-google-id-token"))
                .thenReturn(new GoogleTokenVerifier.GoogleIdentity("new.user@gmail.com", "New User", "google-sub-1", null));
        when(repository.findByGoogleSubject("google-sub-1")).thenReturn(Optional.empty());
        when(repository.findByEmailIgnoreCase("new.user@gmail.com")).thenReturn(Optional.empty());
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthService.LoginResponse response = service.loginWithGoogle("verified-google-id-token");

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.user().email()).isEqualTo("new.user@gmail.com");
        assertThat(response.user().role()).isEqualTo("ADMIN");
    }
}
