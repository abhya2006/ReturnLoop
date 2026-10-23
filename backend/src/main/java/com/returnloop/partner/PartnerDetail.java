package com.returnloop.partner;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PartnerDetail(
        UUID id,
        String name,
        PartnerType partnerType,
        String partnerTypeLabel,
        String description,
        String contactName,
        String contactEmail,
        String contactPhone,
        String serviceableRegions,
        String supportedCategories,
        int processingCapacityPerDay,
        boolean currentlyAvailable,
        BigDecimal feePerUnit,
        BigDecimal feePercentage,
        String acceptanceCriteria,
        BigDecimal latitude,
        BigDecimal longitude,
        String address,
        boolean verified,
        int completedJobs,
        int failedJobs,
        BigDecimal averageProcessingDays,
        BigDecimal acceptanceRate,
        BigDecimal onTimeRate,
        List<RecoveryJobSummary> recentJobs) {

    public static PartnerDetail from(Partner partner, List<RecoveryJob> jobs) {
        return new PartnerDetail(
                partner.getId(),
                partner.getName(),
                partner.getPartnerType(),
                partner.getPartnerType().getLabel(),
                partner.getDescription(),
                partner.getContactName(),
                partner.getContactEmail(),
                partner.getContactPhone(),
                partner.getServiceableRegions(),
                partner.getSupportedCategories(),
                partner.getProcessingCapacityPerDay(),
                partner.isCurrentlyAvailable(),
                partner.getFeePerUnit(),
                partner.getFeePercentage(),
                partner.getAcceptanceCriteria(),
                partner.getLatitude(),
                partner.getLongitude(),
                partner.getAddress(),
                partner.isVerified(),
                partner.getCompletedJobs(),
                partner.getFailedJobs(),
                partner.getAverageProcessingDays(),
                partner.getAcceptanceRate(),
                partner.getOnTimeRate(),
                jobs.stream().map(RecoveryJobSummary::from).toList());
    }
}
