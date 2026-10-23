package com.returnloop.returns;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReturnDetail(
        UUID id,
        String returnNumber,
        ReturnStatus status,
        String statusLabel,
        ReturnReason reason,
        String reasonLabel,
        String sourceReference,
        String customerName,
        String customerEmail,
        String originAddress,
        BigDecimal originLatitude,
        BigDecimal originLongitude,
        UUID warehouseId,
        UUID pickupId,
        String notes,
        int itemCount,
        BigDecimal estimatedRecoveryValue,
        BigDecimal realizedRecoveryValue,
        String cancelledReason,
        Instant createdAt,
        Instant statusChangedAt,
        List<ReturnItemResponse> items,
        List<ReturnEventResponse> timeline) {

    public static ReturnDetail from(ReturnRequest entity, List<ReturnItem> items, List<ReturnEvent> events) {
        return new ReturnDetail(
                entity.getId(),
                entity.getReturnNumber(),
                entity.getStatus(),
                entity.getStatus().getLabel(),
                entity.getReason(),
                entity.getReason().getLabel(),
                entity.getSourceReference(),
                entity.getCustomerName(),
                entity.getCustomerEmail(),
                entity.getOriginAddress(),
                entity.getOriginLatitude(),
                entity.getOriginLongitude(),
                entity.getWarehouseId(),
                entity.getPickupId(),
                entity.getNotes(),
                entity.getItemCount(),
                entity.getEstimatedRecoveryValue(),
                entity.getRealizedRecoveryValue(),
                entity.getCancelledReason(),
                entity.getCreatedAt(),
                entity.getStatusChangedAt(),
                items.stream().map(ReturnItemResponse::from).toList(),
                events.stream().map(ReturnEventResponse::from).toList());
    }
}
