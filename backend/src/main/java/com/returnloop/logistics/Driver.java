package com.returnloop.logistics;

import com.returnloop.common.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A driver available for collection.
 */
@Entity
@Table(name = "drivers",
        indexes = @Index(name = "idx_drivers_org_active", columnList = "organization_id,active"))
@Getter
@Setter
public class Driver extends TenantEntity {

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "license_number", length = 60)
    private String licenseNumber;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
