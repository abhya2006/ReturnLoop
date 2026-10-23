package com.returnloop.notification;

import com.returnloop.common.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * An in-app notification for a user.
 */
@Entity
@Table(name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_user_read", columnList = "user_id,read"),
                @Index(name = "idx_notifications_org", columnList = "organization_id,created_at")
        })
@Getter
@Setter
public class Notification extends TenantEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "title", nullable = false, length = 240)
    private String title;

    @Column(name = "body", length = 2000)
    private String body;

    @Column(name = "category", nullable = false, length = 60)
    private String category = "GENERAL";

    @Column(name = "severity", nullable = false, length = 20)
    private String severity = "INFO";

    @Column(name = "link", length = 500)
    private String link;

    @Column(name = "reference_type", length = 60)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "read", nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private java.time.Instant readAt;
}
