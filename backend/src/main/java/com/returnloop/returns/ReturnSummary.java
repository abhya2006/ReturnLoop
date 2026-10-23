package com.returnloop.returns;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReturnSummary(
        UUID id,
        String returnNumber,
        ReturnStatus status,
        String statusLabel,
        ReturnReason reason,
        String sourceReference,
        String customerName,
        int itemCount,
        BigDecimal estimatedRecoveryValue,
        BigDecimal realizedRecoveryValue,
        Instant createdAt,
        Instant statusChangedAt) {

    public static ReturnSummary from(ReturnRequest entity) {
        return new ReturnSummary(
                entity.getId(),
                entity.getReturnNumber(),
                entity.getStatus(),
                entity.getStatus().getLabel(),
                entity.getReason(),
                entity.getSourceReference(),
                entity.getCustomerName(),
                entity.getItemCount(),
                entity.getEstimatedRecoveryValue(),
                entity.getRealizedRecoveryValue(),
                entity.getCreatedAt(),
                entity.getStatusChangedAt());
    }
}
