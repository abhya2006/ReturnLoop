package com.returnloop.returns;

import com.returnloop.common.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * An immutable audit entry in the return lifecycle timeline.
 */
@Entity
@Table(name = "return_events",
        indexes = @Index(name = "idx_return_events_return", columnList = "return_id,created_at"))
@Getter
@Setter
public class ReturnEvent extends TenantEntity {

    @Column(name = "return_id", nullable = false)
    private UUID returnId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "from_status", length = 40)
    private String fromStatus;

    @Column(name = "to_status", length = 40)
    private String toStatus;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "actor", length = 160)
    private String actor;

    @Column(name = "actor_role", length = 40)
    private String actorRole;
}
