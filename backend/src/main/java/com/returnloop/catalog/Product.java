package com.returnloop.catalog;

import com.returnloop.common.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A catalogued product / SKU. The SKU is unique within an organisation.
 */
@Entity
@Table(name = "products",
        uniqueConstraints = @UniqueConstraint(name = "uq_products_org_sku", columnNames = {"organization_id", "sku"}),
        indexes = {
                @Index(name = "idx_products_org", columnList = "organization_id"),
                @Index(name = "idx_products_category", columnList = "organization_id,category"),
                @Index(name = "idx_products_name", columnList = "name")
        })
@Getter
@Setter
public class Product extends TenantEntity {

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "name", nullable = false, length = 240)
    private String name;

    @Column(name = "brand", length = 120)
    private String brand;

    @Column(name = "model", length = 120)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private ProductCategory category = ProductCategory.OTHER;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "weight_kg", precision = 10, scale = 3)
    private BigDecimal weightKg;

    /** Original retail price; the anchor for all recovery economics. */
    @Column(name = "original_price", precision = 14, scale = 2)
    private BigDecimal originalPrice;

    /** Current expected resale value in the secondary market. */
    @Column(name = "resale_value", precision = 14, scale = 2)
    private BigDecimal resaleValue;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
