package com.returnloop.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InventoryItemDetail(
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
        String notes,
        Instant receivedAt,
        Instant dispatchedAt,
        Instant createdAt,
        List<MovementResponse> movements) {

    public static InventoryItemDetail from(InventoryItem item, List<InventoryMovement> movements) {
        return new InventoryItemDetail(
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
                item.getNotes(),
                item.getReceivedAt(),
                item.getDispatchedAt(),
                item.getCreatedAt(),
                movements.stream().map(MovementResponse::from).toList());
    }

    public record MovementResponse(UUID id, String movementType, int quantity, String fromStatus, String toStatus,
                                   BigDecimal valueDelta, String referenceType, UUID referenceId, String note,
                                   String actor, Instant occurredAt) {
        static MovementResponse from(InventoryMovement movement) {
            return new MovementResponse(
                    movement.getId(),
                    movement.getMovementType().name(),
                    movement.getQuantity(),
                    movement.getFromStatus(),
                    movement.getToStatus(),
                    movement.getValueDelta(),
                    movement.getReferenceType(),
                    movement.getReferenceId(),
                    movement.getNote(),
                    movement.getActor(),
                    movement.getCreatedAt());
        }
    }
}
