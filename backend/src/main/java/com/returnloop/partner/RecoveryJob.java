package com.returnloop.partner;

import com.returnloop.common.domain.TenantEntity;
import com.returnloop.recovery.RecoveryPath;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A unit of recovery work assigned to a partner.
 */
@Entity
@Table(name = "recovery_jobs",
        indexes = {
                @Index(name = "idx_jobs_org_status", columnList = "organization_id,status"),
                @Index(name = "idx_jobs_partner", columnList = "partner_id,status"),
                @Index(name = "idx_jobs_return", columnList = "return_id")
        })
@Getter
@Setter
public class RecoveryJob extends TenantEntity {

    @Column(name = "job_number", nullable = false, length = 40)
    private String jobNumber;

    @Column(name = "return_id")
    private UUID returnId;

    @Column(name = "return_item_id")
    private UUID returnItemId;

    @Column(name = "inventory_item_id")
    private UUID inventoryItemId;

    @Column(name = "sku", length = 64)
    private String sku;

    @Column(name = "product_name", length = 240)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private int quantity = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "recovery_path", nullable = false, length = 40)
    private RecoveryPath recoveryPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private JobStatus status = JobStatus.PROPOSED;

    @Column(name = "partner_id")
    private UUID partnerId;

    @Column(name = "partner_name", length = 200)
    private String partnerName;

    @Column(name = "expected_recovery_value", precision = 14, scale = 2)
    private BigDecimal expectedRecoveryValue;

    @Column(name = "cost_estimate", precision = 14, scale = 2)
    private BigDecimal costEstimate;

    @Column(name = "realized_value", precision = 14, scale = 2)
    private BigDecimal realizedValue;

    @Column(name = "service_deadline")
    private LocalDate serviceDeadline;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "evidence_note", length = 2000)
    private String evidenceNote;

    @Column(name = "exception_note", length = 2000)
    private String exceptionNote;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "progress_note", length = 1000)
    private String progressNote;
}
