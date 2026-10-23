package com.returnloop.partner;

import com.returnloop.recovery.RecoveryPath;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RecoveryJobSummary(
        UUID id,
        String jobNumber,
        UUID returnId,
        UUID returnItemId,
        UUID inventoryItemId,
        String sku,
        String productName,
        int quantity,
        RecoveryPath recoveryPath,
        String recoveryPathLabel,
        JobStatus status,
        String statusLabel,
        UUID partnerId,
        String partnerName,
        BigDecimal expectedRecoveryValue,
        BigDecimal costEstimate,
        BigDecimal realizedValue,
        LocalDate serviceDeadline,
        Instant assignedAt,
        Instant acceptedAt,
        Instant startedAt,
        Instant completedAt,
        String evidenceNote,
        String exceptionNote,
        String rejectionReason,
        String progressNote,
        Instant createdAt) {

    public static RecoveryJobSummary from(RecoveryJob job) {
        return new RecoveryJobSummary(
                job.getId(),
                job.getJobNumber(),
                job.getReturnId(),
                job.getReturnItemId(),
                job.getInventoryItemId(),
                job.getSku(),
                job.getProductName(),
                job.getQuantity(),
                job.getRecoveryPath(),
                job.getRecoveryPath().getLabel(),
                job.getStatus(),
                job.getStatus().getLabel(),
                job.getPartnerId(),
                job.getPartnerName(),
                job.getExpectedRecoveryValue(),
                job.getCostEstimate(),
                job.getRealizedValue(),
                job.getServiceDeadline(),
                job.getAssignedAt(),
                job.getAcceptedAt(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getEvidenceNote(),
                job.getExceptionNote(),
                job.getRejectionReason(),
                job.getProgressNote(),
                job.getCreatedAt());
    }
}
