package com.returnloop.identity.user;

import com.returnloop.common.domain.TenantEntity;
import com.returnloop.security.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A user account. Belongs to exactly one organisation; the {@code organizationId}
 * on the record scopes every query the user can perform.
 */
@Entity
@Table(name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uq_users_org_email", columnNames = {"organization_id", "email"}),
        indexes = {
                @Index(name = "idx_users_org", columnList = "organization_id"),
                @Index(name = "idx_users_partner", columnList = "partner_id")
        })
@Getter
@Setter
public class UserAccount extends TenantEntity {

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 120)
    private String passwordHash;

    @Column(name = "google_subject", length = 255, unique = true)
    private String googleSubject;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private Role role = Role.VIEWER;

    @Column(name = "job_title", length = 120)
    private String jobTitle;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /** Set when the account belongs to an external partner organisation. */
    @Column(name = "partner_id")
    private UUID partnerId;

    public boolean isEnabled() {
        return active;
    }
}
