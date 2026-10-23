package com.returnloop.returns;

import com.returnloop.common.domain.TenantEntity;
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
import java.util.UUID;

/**
 * A return request. The aggregate root of the reverse-logistics lifecycle.
 */
@Entity
@Table(name = "return_requests",
        indexes = {
                @Index(name = "idx_returns_org_status", columnList = "organization_id,status"),
                @Index(name = "idx_returns_org_created", columnList = "organization_id,created_at"),
                @Index(name = "idx_returns_reference", columnList = "organization_id,source_reference")
        })
@Getter
@Setter
public class ReturnRequest extends TenantEntity {

    /** Human friendly identifier, e.g. {@code RET-2026-00042}. */
    @Column(name = "return_number", nullable = false, length = 40)
    private String returnNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private ReturnStatus status = ReturnStatus.RETURN_CREATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 40)
    private ReturnReason reason = ReturnReason.OTHER;

    @Column(name = "source_reference", length = 120)
    private String sourceReference;

    @Column(name = "customer_name", length = 160)
    private String customerName;

    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    @Column(name = "origin_address", length = 500)
    private String originAddress;

    @Column(name = "origin_latitude", precision = 10, scale = 7)
    private BigDecimal originLatitude;

    @Column(name = "origin_longitude", precision = 10, scale = 7)
    private BigDecimal originLongitude;

    @Column(name = "warehouse_id")
    private UUID warehouseId;

    @Column(name = "pickup_id")
    private UUID pickupId;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "estimated_value", precision = 14, scale = 2)
    private BigDecimal estimatedRecoveryValue;

    @Column(name = "realized_value", precision = 14, scale = 2)
    private BigDecimal realizedRecoveryValue;

    @Column(name = "item_count", nullable = false)
    private int itemCount = 0;

    @Column(name = "status_changed_at", nullable = false)
    private Instant statusChangedAt = Instant.now();

    @Column(name = "cancelled_reason", length = 500)
    private String cancelledReason;
}
