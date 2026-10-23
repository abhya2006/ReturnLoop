package com.returnloop.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryItemResponse(
        UUID id,
        String sku,
        UUID productId,
        String productName,
        String category,
        int quantity,
        int reservedQuantity,
        int availableQuantity,
        InventoryStatus status,
        String statusLabel,
        String condition,
        UUID warehouseId,
        String locationCode,
        BigDecimal estimatedValue,
        BigDecimal unitValue,
        BigDecimal weightKg,
        UUID returnId,
        UUID recoveryJobId,
        Instant receivedAt,
        Instant dispatchedAt,
        Instant createdAt) {

    public static InventoryItemResponse from(InventoryItem item) {
        return new InventoryItemResponse(
                item.getId(),
                item.getSku(),
                item.getProductId(),
                item.getProductName(),
                item.getCategory(),
                item.getQuantity(),
                item.getReservedQuantity(),
                Math.max(0, item.getQuantity() - item.getReservedQuantity()),
                item.getStatus(),
                item.getStatus().getLabel(),
                item.getCondition().name(),
                item.getWarehouseId(),
                item.getLocationCode(),
                item.getEstimatedValue(),
                item.getUnitValue(),
                item.getWeightKg(),
                item.getReturnId(),
                item.getRecoveryJobId(),
                item.getReceivedAt(),
                item.getDispatchedAt(),
                item.getCreatedAt());
    }
}
