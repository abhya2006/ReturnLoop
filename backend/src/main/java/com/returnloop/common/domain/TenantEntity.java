package com.returnloop.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.util.UUID;

/**
 * Base type for tenant-owned records. Every query in the application is scoped
 * by {@code organizationId}, which is how organisation-level data isolation is
 * enforced (see {@code TenantRepository} and the service layer).
 */
@MappedSuperclass
public abstract class TenantEntity extends BaseEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }
}
