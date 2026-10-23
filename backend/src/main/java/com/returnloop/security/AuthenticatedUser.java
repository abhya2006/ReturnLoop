package com.returnloop.security;

import java.util.UUID;

/**
 * The authenticated principal attached to the security context. Carries the
 * active organisation so that every query can be tenant scoped.
 */
public record AuthenticatedUser(
        UUID id,
        String email,
        String fullName,
        UUID organizationId,
        Role role) {
}
