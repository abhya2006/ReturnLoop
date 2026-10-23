package com.returnloop.security;

import com.returnloop.identity.user.UserAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-value-that-is-long-enough-for-hmac-sha-256";

    private UserAccount user() {
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        user.setOrganizationId(UUID.randomUUID());
        user.setEmail("ops@northwind-retail.example");
        user.setFullName("Amara Okafor");
        user.setRole(Role.OPERATIONS_MANAGER);
        return user;
    }

    @Test
    @DisplayName("an issued token round-trips back to the principal")
    void tokenRoundTrips() {
        JwtService service = new JwtService(SECRET, Duration.ofHours(1));
        UserAccount user = user();

        String token = service.issue(user, user.getOrganizationId());
        Optional<AuthenticatedUser> parsed = service.parse(token);

        assertThat(parsed).isPresent();
        assertThat(parsed.get().id()).isEqualTo(user.getId());
        assertThat(parsed.get().organizationId()).isEqualTo(user.getOrganizationId());
        assertThat(parsed.get().role()).isEqualTo(Role.OPERATIONS_MANAGER);
        assertThat(parsed.get().email()).isEqualTo("ops@northwind-retail.example");
    }

    @Test
    @DisplayName("a token signed with a different key is rejected")
    void foreignTokenIsRejected() {
        JwtService issuer = new JwtService(SECRET, Duration.ofHours(1));
        JwtService verifier = new JwtService("a-completely-different-secret-of-sufficient-length", Duration.ofHours(1));

        String token = issuer.issue(user(), UUID.randomUUID());

        assertThat(verifier.parse(token)).isEmpty();
    }

    @Test
    @DisplayName("a malformed token is rejected rather than throwing")
    void malformedTokenIsRejected() {
        JwtService service = new JwtService(SECRET, Duration.ofHours(1));
        assertThat(service.parse("not-a-jwt")).isEmpty();
        assertThat(service.parse("")).isEmpty();
    }

    @Test
    @DisplayName("an expired token is rejected")
    void expiredTokenIsRejected() throws InterruptedException {
        JwtService service = new JwtService(SECRET, Duration.ofMillis(1));
        String token = service.issue(user(), UUID.randomUUID());
        Thread.sleep(1200);
        assertThat(service.parse(token)).isEmpty();
    }

    @Test
    @DisplayName("a short secret is refused at construction time")
    void shortSecretIsRefused() {
        assertThatThrownBy(() -> new JwtService("too-short", Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}
