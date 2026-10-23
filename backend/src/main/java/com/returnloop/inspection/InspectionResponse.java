package com.returnloop.inspection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InspectionResponse(
        UUID id,
        UUID returnId,
        UUID returnItemId,
        String sku,
        String productName,
        InspectionStatus status,
        String statusLabel,
        String packagingCondition,
        String functionalityResult,
        boolean functional,
        boolean accessoriesComplete,
        String damageDescription,
        String missingAccessories,
        BigDecimal estimatedRepairCost,
        boolean flaggedUnsafe,
        String inspectedBy,
        Instant inspectedAt,
        Instant createdAt) {

    public static InspectionResponse from(Inspection inspection) {
        return new InspectionResponse(
                inspection.getId(),
                inspection.getReturnId(),
                inspection.getReturnItemId(),
                inspection.getSku(),
                inspection.getProductName(),
                inspection.getStatus(),
                inspection.getStatus().getLabel(),
                inspection.getPackagingCondition() == null ? null : inspection.getPackagingCondition().name(),
                inspection.getFunctionalityResult() == null ? null : inspection.getFunctionalityResult().name(),
                inspection.isFunctional(),
                inspection.isAccessoriesComplete(),
                inspection.getDamageDescription(),
                inspection.getMissingAccessories(),
                inspection.getEstimatedRepairCost(),
                inspection.isFlaggedUnsafe(),
                inspection.getInspectedBy(),
                inspection.getInspectedAt(),
                inspection.getCreatedAt());
    }
}
