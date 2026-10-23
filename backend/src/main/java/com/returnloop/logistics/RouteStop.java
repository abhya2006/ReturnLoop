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
import java.util.UUID;

/**
 * A single stop on a route.
 */
@Entity
@Table(name = "route_stops",
        indexes = @Index(name = "idx_route_stops_route", columnList = "route_id,sequence"))
@Getter
@Setter
public class RouteStop extends TenantEntity {

    @Column(name = "route_id", nullable = false)
    private UUID routeId;

    @Column(name = "pickup_job_id")
    private UUID pickupJobId;

    @Column(name = "sequence", nullable = false)
    private int sequence;

    @Column(name = "stop_type", nullable = false, length = 40)
    private String stopType = "PICKUP";

    @Column(name = "location_name", nullable = false, length = 200)
    private String locationName;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "unit_count", nullable = false)
    private int unitCount = 0;

    @Column(name = "leg_distance_km", precision = 10, scale = 3)
    private BigDecimal legDistanceKm;

    @Column(name = "leg_duration_minutes")
    private Integer legDurationMinutes;

    @Column(name = "completed", nullable = false)
    private boolean completed = false;

    @Column(name = "completed_at")
    private java.time.Instant completedAt;

    @Column(name = "note", length = 500)
    private String note;
}
