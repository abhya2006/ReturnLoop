package com.returnloop.returns;

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
 * A single product line inside a return request.
 */
@Entity
@Table(name = "return_items",
        indexes = {
                @Index(name = "idx_return_items_return", columnList = "return_id"),
                @Index(name = "idx_return_items_product", columnList = "organization_id,product_id")
        })
@Getter
@Setter
public class ReturnItem extends TenantEntity {

    @Column(name = "return_id", nullable = false)
    private UUID returnId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "product_name", nullable = false, length = 240)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private int quantity = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition", nullable = false, length = 40)
    private ProductCondition condition = ProductCondition.GOOD;

    @Column(name = "serial_number", length = 120)
    private String serialNumber;

    @Column(name = "accessories_complete", nullable = false)
    private boolean accessoriesComplete = true;

    @Column(name = "estimated_value", precision = 14, scale = 2)
    private BigDecimal estimatedValue;

    @Column(name = "notes", length = 1000)
    private String notes;
}
