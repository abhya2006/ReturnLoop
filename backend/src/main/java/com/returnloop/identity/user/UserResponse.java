package com.returnloop.identity.user;

import com.returnloop.security.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        Role role,
        String roleLabel,
        String jobTitle,
        boolean active,
        Instant lastLoginAt,
        Instant createdAt) {

    public static UserResponse from(UserAccount user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getRole().name(),
                user.getJobTitle(),
                user.isActive(),
                user.getLastLoginAt(),
                user.getCreatedAt());
    }
}
