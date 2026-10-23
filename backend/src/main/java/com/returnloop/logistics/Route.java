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
import java.time.LocalDate;
import java.util.UUID;

/**
 * A planned collection route for one vehicle.
 */
@Entity
@Table(name = "routes",
        indexes = {
                @Index(name = "idx_routes_org_date", columnList = "organization_id,route_date"),
                @Index(name = "idx_routes_vehicle", columnList = "vehicle_id")
        })
@Getter
@Setter
public class Route extends TenantEntity {

    @Column(name = "route_number", nullable = false, length = 40)
    private String routeNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private RouteStatus status = RouteStatus.PLANNED;

    @Column(name = "route_date")
    private LocalDate routeDate;

    @Column(name = "vehicle_id")
    private UUID vehicleId;

    @Column(name = "vehicle_name", length = 120)
    private String vehicleName;

    @Column(name = "driver_id")
    private UUID driverId;

    @Column(name = "driver_name", length = 160)
    private String driverName;

    @Column(name = "origin_location_id")
    private UUID originLocationId;

    @Column(name = "origin_name", length = 200)
    private String originName;

    @Column(name = "stop_count", nullable = false)
    private int stopCount = 0;

    @Column(name = "total_units", nullable = false)
    private int totalUnits = 0;

    @Column(name = "total_weight_kg", precision = 10, scale = 3)
    private BigDecimal totalWeightKg;

    @Column(name = "estimated_distance_km", precision = 10, scale = 3)
    private BigDecimal estimatedDistanceKm;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "estimated_cost", precision = 14, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "algorithm", nullable = false, length = 60)
    private String algorithm = "nearest-neighbor-2opt";

    @Column(name = "notes", length = 1000)
    private String notes;
}
