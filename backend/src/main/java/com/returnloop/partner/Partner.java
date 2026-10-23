package com.returnloop.partner;

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
 * A recovery partner. Performance metrics on this record are only ever
 * written by the system from completed jobs, never hand-entered.
 */
@Entity
@Table(name = "partners",
        indexes = {
                @Index(name = "idx_partners_org_type", columnList = "organization_id,partner_type"),
                @Index(name = "idx_partners_org_name", columnList = "organization_id,name")
        })
@Getter
@Setter
public class Partner extends TenantEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "partner_type", nullable = false, length = 40)
    private PartnerType partnerType;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "contact_name", length = 160)
    private String contactName;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "contact_phone", length = 40)
    private String contactPhone;

    /** Comma separated serviceable regions, e.g. {@code US-NORTHEAST,US-MIDWEST}. */
    @Column(name = "serviceable_regions", length = 1000)
    private String serviceableRegions;

    /** Comma separated product categories the partner handles. */
    @Column(name = "supported_categories", length = 1000)
    private String supportedCategories;

    /** Maximum units the partner can process per day. */
    @Column(name = "processing_capacity_per_day", nullable = false)
    private int processingCapacityPerDay = 50;

    @Column(name = "currently_available", nullable = false)
    private boolean currentlyAvailable = true;

    /** Fee charged per unit, or a percentage when {@code feePercentage} is set. */
    @Column(name = "fee_per_unit", precision = 14, scale = 2)
    private BigDecimal feePerUnit;

    @Column(name = "fee_percentage", precision = 5, scale = 2)
    private BigDecimal feePercentage;

    @Column(name = "acceptance_criteria", length = 2000)
    private String acceptanceCriteria;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    // ---- Metrics derived from completed jobs (never manually entered) ----

    @Column(name = "completed_jobs", nullable = false)
    private int completedJobs = 0;

    @Column(name = "failed_jobs", nullable = false)
    private int failedJobs = 0;

    /** Average processing time in days across completed jobs. */
    @Column(name = "average_processing_days", precision = 8, scale = 2)
    private BigDecimal averageProcessingDays;

    /** Share of assigned jobs accepted, 0-100. */
    @Column(name = "acceptance_rate", precision = 5, scale = 2)
    private BigDecimal acceptanceRate;

    /** Share of completed jobs that met the deadline, 0-100. */
    @Column(name = "on_time_rate", precision = 5, scale = 2)
    private BigDecimal onTimeRate;
}
