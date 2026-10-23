package com.returnloop.identity.auth;

import com.returnloop.common.error.ApiException;
import com.returnloop.common.error.ErrorCode;
import com.returnloop.identity.user.UserAccount;
import com.returnloop.identity.user.UserAccountRepository;
import com.returnloop.organization.Organization;
import com.returnloop.organization.OrganizationRepository;
import com.returnloop.security.Role;
import com.returnloop.security.AuthenticatedUser;
import com.returnloop.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Authenticates users and issues access tokens.
 */
@Service
public class AuthService {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OrganizationRepository organizationRepository;
    private final JavaMailSender mailSender;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final String mailFrom;
    private final Map<String, ResetChallenge> resetChallenges = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserAccountRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       OrganizationRepository organizationRepository, JavaMailSender mailSender,
                       GoogleTokenVerifier googleTokenVerifier,
                       @Value("${returnloop.auth.mail-from:no-reply@returnloop.example}") String mailFrom) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.organizationRepository = organizationRepository;
        this.mailSender = mailSender;
        this.googleTokenVerifier = googleTokenVerifier;
        this.mailFrom = mailFrom;
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw ApiException.conflict("An account with this email already exists");
        }
        Organization organization = new Organization();
        organization.setName(request.organizationName().trim());
        organization.setContactEmail(email);
        organization.setSlug(slug(request.organizationName()) + "-" + UUID.randomUUID().toString().substring(0, 8));
        Organization savedOrganization = organizationRepository.save(organization);

        UserAccount user = new UserAccount();
        user.setOrganizationId(savedOrganization.getId());
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setRole(Role.ADMIN);
        user.setActive(true);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);
        return loginResponse(user, savedOrganization.getId());
    }

    @Transactional
    public LoginResponse loginWithGoogle(String credential) {
        GoogleTokenVerifier.GoogleIdentity identity = googleTokenVerifier.verify(credential);
        UserAccount user = userRepository.findByGoogleSubject(identity.subject()).orElseGet(() -> {
            var sameEmail = userRepository.findByEmailIgnoreCase(identity.email());
            if (sameEmail.isPresent() && !identity.email().endsWith("@gmail.com")
                    && (identity.hostedDomain() == null || identity.hostedDomain().isBlank())) {
                throw new ApiException(ErrorCode.INVALID_CREDENTIALS,
                        "Sign in with your password first to connect this Google account");
            }
            if (sameEmail.isPresent() && sameEmail.get().getGoogleSubject() != null
                    && !sameEmail.get().getGoogleSubject().equals(identity.subject())) {
                throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
            }
            if (sameEmail.isPresent()) {
                UserAccount linked = sameEmail.get();
                linked.setGoogleSubject(identity.subject());
                return linked;
            }
            String fullName = identity.name() == null || identity.name().isBlank()
                    ? identity.email().substring(0, identity.email().indexOf('@')) : identity.name().trim();
            fullName = fullName.substring(0, Math.min(fullName.length(), 160));
            Organization organization = new Organization();
            organization.setName((fullName + "'s workspace").substring(0,
                    Math.min((fullName + "'s workspace").length(), 160)));
            organization.setContactEmail(identity.email());
            organization.setSlug(slug(fullName) + "-" + UUID.randomUUID().toString().substring(0, 8));
            Organization savedOrganization = organizationRepository.save(organization);

            UserAccount created = new UserAccount();
            created.setOrganizationId(savedOrganization.getId());
            created.setEmail(identity.email());
            created.setFullName(fullName);
            created.setRole(Role.ADMIN);
            created.setActive(true);
            created.setGoogleSubject(identity.subject());
            created.setPasswordHash(passwordEncoder.encode(UUID.randomUUID() + UUID.randomUUID().toString()));
            return userRepository.save(created);
        });
        if (!user.getEmail().equalsIgnoreCase(identity.email())) user.setEmail(identity.email());
        if (!user.isActive()) throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        return loginResponse(user, user.getOrganizationId());
    }

    private LoginResponse loginResponse(UserAccount user, UUID organizationId) {
        return new LoginResponse(jwtService.issue(user, organizationId), "Bearer",
                jwtService.accessTokenTtlSeconds(), new UserProfile(user.getId(), user.getEmail(), user.getFullName(),
                user.getRole().name(), user.getJobTitle(), organizationId));
    }

    public ForgotPasswordResponse requestPasswordReset(String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        var user = userRepository.findByEmailIgnoreCase(normalized);
        if (user.isPresent()) {
            String code = String.format("%06d", secureRandom.nextInt(1_000_000));
            resetChallenges.put(normalized, new ResetChallenge(passwordEncoder.encode(code), Instant.now().plus(Duration.ofMinutes(15)), false, 0));
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(normalized);
            message.setSubject("Your ReturnLoop password reset code");
            message.setText("Your ReturnLoop password reset code is " + code
                    + ". It expires in 15 minutes. If you did not request this, you can ignore this email.");
            try {
                mailSender.send(message);
            } catch (MailException exception) {
                resetChallenges.remove(normalized);
                throw new ApiException(ErrorCode.EMAIL_DELIVERY_FAILED, ErrorCode.EMAIL_DELIVERY_FAILED.getDefaultMessage(), exception);
            }
        }
        return new ForgotPasswordResponse("If an account exists for that address, a reset code has been sent.");
    }

    public void verifyPasswordResetCode(String email, String code) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        ResetChallenge challenge = resetChallenges.get(normalized);
        if (!isValidChallenge(challenge, code)) {
            resetChallenges.computeIfPresent(normalized, (key, current) -> {
                if (Instant.now().isAfter(current.expiresAt()) || current.failedAttempts() >= 4) return null;
                return new ResetChallenge(current.codeHash(), current.expiresAt(), current.verified(), current.failedAttempts() + 1);
            });
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "The reset code is invalid or has expired");
        }
        resetChallenges.replace(normalized, challenge,
                new ResetChallenge(challenge.codeHash(), challenge.expiresAt(), true, challenge.failedAttempts()));
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        ResetChallenge challenge = resetChallenges.get(normalized);
        if (challenge == null || !challenge.verified() || !isValidChallenge(challenge, code)) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "The reset code is invalid or has expired");
        }
        UserAccount user = userRepository.findByEmailIgnoreCase(normalized)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        resetChallenges.remove(normalized);
    }

    private boolean isValidChallenge(ResetChallenge challenge, String code) {
        return challenge != null && Instant.now().isBefore(challenge.expiresAt())
                && code != null && passwordEncoder.matches(code, challenge.codeHash());
    }

    private String slug(String value) {
        String result = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        return result.isBlank() ? "workspace" : result.substring(0, Math.min(result.length(), 70));
    }

    private record ResetChallenge(String codeHash, Instant expiresAt, boolean verified, int failedAttempts) { }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        if (request.email() == null || request.password() == null) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        UserAccount user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));
        if (!user.isActive()) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "This account has been deactivated");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String token = jwtService.issue(user, user.getOrganizationId());
        return new LoginResponse(
                token,
                "Bearer",
                jwtService.accessTokenTtlSeconds(),
                new UserProfile(
                        user.getId(),
                        user.getEmail(),
                        user.getFullName(),
                        user.getRole().name(),
                        user.getJobTitle(),
                        user.getOrganizationId()));
    }

    @Transactional(readOnly = true)
    public UserProfile profile(AuthenticatedUser principal) {
        UserAccount user = userRepository.findById(principal.id())
                .orElseThrow(() -> ApiException.notFound("User", principal.id()));
        return new UserProfile(user.getId(), user.getEmail(), user.getFullName(), user.getRole().name(),
                user.getJobTitle(), user.getOrganizationId());
    }

    public record LoginRequest(String email, String password) {
    }
    public record RegisterRequest(String organizationName, String fullName, String email, String password) { }
    public record GoogleLoginRequest(String credential) { }
    public record ForgotPasswordResponse(String message) { }

    public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserProfile user) {
    }

    public record UserProfile(UUID id, String email, String fullName, String role, String jobTitle,
                              UUID organizationId) {
    }
}
