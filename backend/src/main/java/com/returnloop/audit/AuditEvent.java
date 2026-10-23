package com.returnloop.audit;

import com.returnloop.common.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * An immutable audit record of a privileged action.
 */
@Entity
@Table(name = "audit_events",
        indexes = {
                @Index(name = "idx_audit_org_created", columnList = "organization_id,created_at"),
                @Index(name = "idx_audit_entity", columnList = "entity_type,entity_id"),
                @Index(name = "idx_audit_actor", columnList = "actor")
        })
@Getter
@Setter
public class AuditEvent extends TenantEntity {

    @Column(name = "actor", nullable = false, length = 160)
    private String actor;

    @Column(name = "actor_role", length = 40)
    private String actorRole;

    @Column(name = "action", nullable = false, length = 80)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 60)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "summary", length = 1000)
    private String summary;

    @Column(name = "details", columnDefinition = "text")
    private String details;

    @Column(name = "ip_address", length = 60)
    private String ipAddress;
}
