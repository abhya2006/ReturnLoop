package com.returnloop.inventory;

import com.returnloop.common.domain.TenantEntity;
import com.returnloop.returns.ProductCondition;
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
import java.util.UUID;

/**
 * Goods held in a warehouse. Inventory is the bridge between a return and the
 * recovery work that recovers value from it.
 */
@Entity
@Table(name = "inventory_items",
        indexes = {
                @Index(name = "idx_inventory_org_status", columnList = "organization_id,status"),
                @Index(name = "idx_inventory_org_sku", columnList = "organization_id,sku"),
                @Index(name = "idx_inventory_warehouse", columnList = "warehouse_id")
        })
@Getter
@Setter
public class InventoryItem extends TenantEntity {

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "product_name", nullable = false, length = 240)
    private String productName;

    @Column(name = "category", length = 40)
    private String category;

    @Column(name = "quantity", nullable = false)
    private int quantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private InventoryStatus status = InventoryStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition", nullable = false, length = 40)
    private ProductCondition condition = ProductCondition.GOOD;

    @Column(name = "warehouse_id")
    private UUID warehouseId;

    @Column(name = "location_code", length = 40)
    private String locationCode;

    @Column(name = "estimated_value", precision = 14, scale = 2)
    private BigDecimal estimatedValue;

    @Column(name = "unit_value", precision = 14, scale = 2)
    private BigDecimal unitValue;

    @Column(name = "weight_kg", precision = 10, scale = 3)
    private BigDecimal weightKg;

    @Column(name = "return_id")
    private UUID returnId;

    @Column(name = "recovery_job_id")
    private UUID recoveryJobId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Column(name = "notes", length = 1000)
    private String notes;
}
