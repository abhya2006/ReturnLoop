package com.returnloop.logistics;

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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * A request to collect goods from an origin and deliver them to a warehouse.
 */
@Entity
@Table(name = "pickup_jobs",
        indexes = {
                @Index(name = "idx_pickups_org_status", columnList = "organization_id,status"),
                @Index(name = "idx_pickups_date", columnList = "scheduled_date,status")
        })
@Getter
@Setter
public class PickupJob extends TenantEntity {

    @Column(name = "pickup_number", nullable = false, length = 40)
    private String pickupNumber;

    @Column(name = "return_id")
    private UUID returnId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private PickupStatus status = PickupStatus.REQUESTED;

    @Column(name = "origin_address", length = 500)
    private String originAddress;

    @Column(name = "origin_latitude", precision = 10, scale = 7)
    private BigDecimal originLatitude;

    @Column(name = "origin_longitude", precision = 10, scale = 7)
    private BigDecimal originLongitude;

    @Column(name = "destination_location_id")
    private UUID destinationLocationId;

    @Column(name = "destination_name", length = 200)
    private String destinationName;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "window_start")
    private LocalTime windowStart;

    @Column(name = "window_end")
    private LocalTime windowEnd;

    @Column(name = "priority", nullable = false)
    private int priority = 5;

    @Column(name = "unit_count", nullable = false)
    private int unitCount = 1;

    @Column(name = "total_weight_kg", precision = 10, scale = 3)
    private BigDecimal totalWeightKg;

    @Column(name = "vehicle_id")
    private UUID vehicleId;

    @Column(name = "driver_id")
    private UUID driverId;

    @Column(name = "route_id")
    private UUID routeId;

    @Column(name = "stop_sequence")
    private Integer stopSequence;

    @Column(name = "estimated_distance_km", precision = 10, scale = 3)
    private BigDecimal estimatedDistanceKm;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "estimated_cost", precision = 14, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "evidence_note", length = 2000)
    private String evidenceNote;

    @Column(name = "notes", length = 1000)
    private String notes;
}
