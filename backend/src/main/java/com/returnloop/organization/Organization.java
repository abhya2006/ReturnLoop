package com.returnloop.organization;

import com.returnloop.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * A tenant. Every operational record in the platform belongs to one.
 */
@Entity
@Table(name = "organizations",
        uniqueConstraints = @UniqueConstraint(name = "uq_organizations_slug", columnNames = "slug"))
@Getter
@Setter
public class Organization extends BaseEntity {

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "slug", nullable = false, length = 80)
    private String slug;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "country", length = 80)
    private String country;

    @Column(name = "default_currency", nullable = false, length = 3)
    private String defaultCurrency = "USD";

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Daily holding-cost threshold in days used by the inventory aging monitor. */
    @Column(name = "holding_threshold_days", nullable = false)
    private int holdingThresholdDays = 30;
}
