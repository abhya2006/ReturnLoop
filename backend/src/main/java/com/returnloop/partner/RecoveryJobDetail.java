package com.returnloop.partner;

import com.returnloop.recovery.RecoveryPath;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RecoveryJobDetail(
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
        Instant createdAt,
        List<JobEventResponse> history) {

    public static RecoveryJobDetail from(RecoveryJob job, List<RecoveryJobEvent> events) {
        return new RecoveryJobDetail(
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
                job.getCreatedAt(),
                events.stream().map(JobEventResponse::from).toList());
    }

    public record JobEventResponse(UUID id, String eventType, String fromStatus, String toStatus,
                                   String description, String actor, Instant occurredAt) {
        static JobEventResponse from(RecoveryJobEvent event) {
            return new JobEventResponse(event.getId(), event.getEventType(), event.getFromStatus(),
                    event.getToStatus(), event.getDescription(), event.getActor(), event.getCreatedAt());
        }
    }
}
