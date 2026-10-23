package com.returnloop.inventory;

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
 * An immutable record of a stock movement.
 */
@Entity
@Table(name = "inventory_movements",
        indexes = @Index(name = "idx_movements_item", columnList = "inventory_item_id,created_at"))
@Getter
@Setter
public class InventoryMovement extends TenantEntity {

    @Column(name = "inventory_item_id", nullable = false)
    private UUID inventoryItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 40)
    private MovementType movementType;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "from_status", length = 40)
    private String fromStatus;

    @Column(name = "to_status", length = 40)
    private String toStatus;

    @Column(name = "value_delta", precision = 14, scale = 2)
    private BigDecimal valueDelta;

    @Column(name = "reference_type", length = 60)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "actor", length = 160)
    private String actor;

    public enum MovementType {
        RECEIPT,
        ALLOCATION,
        RELEASE,
        DISPATCH,
        RECEIPT_BACK,
        ADJUSTMENT,
        RESERVATION,
        SCRAP
    }
}
