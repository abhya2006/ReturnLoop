package com.returnloop.inspection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InspectionDetail(
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
        String notes,
        boolean flaggedUnsafe,
        String inspectedBy,
        Instant inspectedAt,
        Instant createdAt,
        List<AttachmentResponse> attachments) {

    public static InspectionDetail from(Inspection inspection, List<InspectionAttachment> attachments) {
        return new InspectionDetail(
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
                inspection.getNotes(),
                inspection.isFlaggedUnsafe(),
                inspection.getInspectedBy(),
                inspection.getInspectedAt(),
                inspection.getCreatedAt(),
                attachments.stream().map(AttachmentResponse::from).toList());
    }

    public record AttachmentResponse(UUID id, String fileName, String contentType, long sizeBytes,
                                     String caption, Instant createdAt) {
        static AttachmentResponse from(InspectionAttachment attachment) {
            return new AttachmentResponse(attachment.getId(), attachment.getFileName(),
                    attachment.getContentType(), attachment.getSizeBytes(), attachment.getCaption(),
                    attachment.getCreatedAt());
        }
    }
}
