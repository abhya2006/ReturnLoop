package com.returnloop.partner;

import com.returnloop.common.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Status history for a recovery job.
 */
@Entity
@Table(name = "recovery_job_events",
        indexes = @Index(name = "idx_job_events_job", columnList = "job_id,created_at"))
@Getter
@Setter
public class RecoveryJobEvent extends TenantEntity {

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

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
}
