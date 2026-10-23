package com.returnloop.logistics;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record PickupJobResponse(
        UUID id,
        String pickupNumber,
        UUID returnId,
        PickupStatus status,
        String statusLabel,
        String originAddress,
        BigDecimal originLatitude,
        BigDecimal originLongitude,
        UUID destinationLocationId,
        String destinationName,
        LocalDate scheduledDate,
        LocalTime windowStart,
        LocalTime windowEnd,
        int priority,
        int unitCount,
        BigDecimal totalWeightKg,
        UUID vehicleId,
        UUID driverId,
        UUID routeId,
        Integer stopSequence,
        BigDecimal estimatedDistanceKm,
        Integer estimatedDurationMinutes,
        BigDecimal estimatedCost,
        Instant completedAt,
        String failureReason,
        String evidenceNote,
        String notes,
        Instant createdAt) {

    public static PickupJobResponse from(PickupJob job) {
        return new PickupJobResponse(
                job.getId(),
                job.getPickupNumber(),
                job.getReturnId(),
                job.getStatus(),
                job.getStatus().getLabel(),
                job.getOriginAddress(),
                job.getOriginLatitude(),
                job.getOriginLongitude(),
                job.getDestinationLocationId(),
                job.getDestinationName(),
                job.getScheduledDate(),
                job.getWindowStart(),
                job.getWindowEnd(),
                job.getPriority(),
                job.getUnitCount(),
                job.getTotalWeightKg(),
                job.getVehicleId(),
                job.getDriverId(),
                job.getRouteId(),
                job.getStopSequence(),
                job.getEstimatedDistanceKm(),
                job.getEstimatedDurationMinutes(),
                job.getEstimatedCost(),
                job.getCompletedAt(),
                job.getFailureReason(),
                job.getEvidenceNote(),
                job.getNotes(),
                job.getCreatedAt());
    }
}
