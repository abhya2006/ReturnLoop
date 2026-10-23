package com.returnloop.logistics;

import com.returnloop.common.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A vehicle available for pickups.
 */
@Entity
@Table(name = "vehicles",
        indexes = @Index(name = "idx_vehicles_org_active", columnList = "organization_id,active"))
@Getter
@Setter
public class Vehicle extends TenantEntity {

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "registration", nullable = false, length = 40)
    private String registration;

    @Column(name = "vehicle_type", length = 60)
    private String vehicleType = "VAN";

    /** Maximum number of units the vehicle can carry. */
    @Column(name = "capacity_units", nullable = false)
    private int capacityUnits = 50;

    /** Maximum payload in kilograms. */
    @Column(name = "capacity_kg", precision = 10, scale = 2)
    private BigDecimal capacityKg;

    @Column(name = "home_location_id")
    private java.util.UUID homeLocationId;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "notes", length = 500)
    private String notes;
}
