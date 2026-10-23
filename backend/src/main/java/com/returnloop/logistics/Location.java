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

/**
 * A physical location: a warehouse, a partner facility or a customer site.
 */
@Entity
@Table(name = "locations",
        indexes = @Index(name = "idx_locations_org_type", columnList = "organization_id,location_type"))
@Getter
@Setter
public class Location extends TenantEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 40)
    private LocationType locationType = LocationType.WAREHOUSE;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "region", length = 80)
    private String region;

    @Column(name = "capacity_units")
    private Integer capacityUnits;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
