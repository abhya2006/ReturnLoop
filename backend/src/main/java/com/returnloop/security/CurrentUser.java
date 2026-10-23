package com.returnloop.security;

import com.returnloop.common.error.ApiException;
import com.returnloop.common.error.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Convenience accessor for the currently authenticated principal.
 */
@Component
public class CurrentUser {

    public Optional<AuthenticatedUser> get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    public AuthenticatedUser require() {
        return get().orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
    }

    public UUID organizationId() {
        return require().organizationId();
    }

    public UUID userId() {
        return require().id();
    }

    public boolean hasRole(Role role) {
        return get().map(user -> user.role() == role).orElse(false);
    }

    /**
     * Returns true when the caller is allowed to perform privileged operations
     * such as approving recovery recommendations or cancelling returns.
     */
    public boolean isPrivileged() {
        return get().map(user -> switch (user.role()) {
            case ADMIN, OPERATIONS_MANAGER -> true;
            default -> false;
        }).orElse(false);
    }
}
