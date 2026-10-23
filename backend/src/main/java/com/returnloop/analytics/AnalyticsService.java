package com.returnloop.analytics;

import com.returnloop.common.web.PageResponse;
import com.returnloop.inventory.InventoryItemRepository;
import com.returnloop.inventory.InventoryStatus;
import com.returnloop.logistics.PickupJob;
import com.returnloop.logistics.PickupJobRepository;
import com.returnloop.logistics.PickupStatus;
import com.returnloop.partner.JobStatus;
import com.returnloop.partner.RecoveryJob;
import com.returnloop.partner.RecoveryJobRepository;
import com.returnloop.recovery.RecoveryOption;
import com.returnloop.recovery.RecoveryPath;
import com.returnloop.recovery.RecoveryRecommendation;
import com.returnloop.recovery.RecoveryRecommendationRepository;
import com.returnloop.returns.ReturnReason;
import com.returnloop.returns.ReturnRequest;
import com.returnloop.returns.ReturnRequestRepository;
import com.returnloop.returns.ReturnStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Aggregates operational data into the dashboard and reporting views.
 *
 * <p>Every figure is computed from persisted records. Nothing is hard-coded.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ReturnRequestRepository returnRepository;
    private final RecoveryRecommendationRepository recommendationRepository;
    private final RecoveryJobRepository jobRepository;
    private final PickupJobRepository pickupRepository;
    private final InventoryItemRepository inventoryRepository;

    /**
     * The five headline KPI cards, each with a comparison against the previous
     * period of the same length.
     */
    @Transactional(readOnly = true)
    public DashboardSummary dashboard(UUID organizationId, LocalDate from, LocalDate to) {
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        long periodDays = Math.max(ChronoUnit.DAYS.between(from, to) + 1, 1);
        Instant previousFrom = fromInstant.minus(Duration.ofDays(periodDays));
        Instant previousTo = fromInstant;

        List<ReturnRequest> periodReturns = returnRepository.findCreatedBetween(organizationId, fromInstant, toInstant);
        List<ReturnRequest> previousReturns = returnRepository
                .findCreatedBetween(organizationId, previousFrom, previousTo);

        long totalReturns = periodReturns.size();
        long previousTotal = previousReturns.size();

        long awaitingInspection = returnRepository
                .countByOrganizationIdAndStatusIn(organizationId,
                        List.of(ReturnStatus.RETURN_CREATED, ReturnStatus.PICKUP_SCHEDULED,
                                ReturnStatus.COLLECTED, ReturnStatus.WAREHOUSE_RECEIVED));

        BigDecimal recoveredValue = periodReturns.stream()
                .map(ReturnRequest::getRealizedRecoveryValue)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal previousRecovered = previousReturns.stream()
                .map(ReturnRequest::getRealizedRecoveryValue)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal wasteAvoided = wasteAvoidedValue(periodReturns);

        Double averageTurnaround = averageTurnaroundDays(periodReturns);

        List<TrendPoint> trend = trendSeries(organizationId, from, to);
        List<DistributionSlice> distribution = recoveryDistribution(organizationId, fromInstant, toInstant);
        List<PickupMapMarker> markers = pickupMarkers(organizationId);

        return new DashboardSummary(
                from.toString(),
                to.toString(),
                new KpiCard("total-returns", "Total Returns", totalReturns, (double) previousTotal,
                        "returns created in the selected period"),
                new KpiCard("awaiting-inspection", "Awaiting Inspection", awaitingInspection, null,
                        "returns waiting for a warehouse inspection"),
                new KpiCard("recovered-value", "Recovered Value", recoveredValue.doubleValue(),
                        previousRecovered.doubleValue(), "value realised from completed recovery"),
                new KpiCard("waste-avoided", "Waste Avoided", wasteAvoided.doubleValue(), null,
                        "gross value kept in circulation rather than written off"),
                new KpiCard("average-turnaround", "Average Turnaround (days)",
                        averageTurnaround == null ? 0 : averageTurnaround, null,
                        "mean days from return creation to recovery"),
                trend,
                distribution,
                markers,
                periodReturns.stream()
                        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                        .limit(8)
                        .map(ReturnRow::from)
                        .toList());
    }

    /**
     * A daily series of returns created against items recovered.
     */
    @Transactional(readOnly = true)
    public List<TrendPoint> trendSeries(UUID organizationId, LocalDate from, LocalDate to) {
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<ReturnRequest> returns = returnRepository.findCreatedBetween(organizationId, fromInstant, toInstant);

        Map<LocalDate, Long> createdByDay = returns.stream()
                .collect(Collectors.groupingBy(r -> r.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate(),
                        Collectors.counting()));

        List<RecoveryRecommendation> recommendations = recommendationRepository
                .findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .filter(r -> r.getCreatedAt().isAfter(fromInstant) && r.getCreatedAt().isBefore(toInstant))
                .toList();
        Map<LocalDate, Long> recoveredByDay = recommendations.stream()
                .filter(r -> r.isApproved() || r.getRecommendedPath() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate(), Collectors.counting()));

        List<TrendPoint> points = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            points.add(new TrendPoint(day.toString(),
                    createdByDay.getOrDefault(day, 0L),
                    recoveredByDay.getOrDefault(day, 0L)));
        }
        return points;
    }

    /**
     * The distribution of recommended recovery paths.
     */
    @Transactional(readOnly = true)
    public List<DistributionSlice> recoveryDistribution(UUID organizationId, Instant from, Instant to) {
        List<RecoveryRecommendation> recommendations = recommendationRepository
                .findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .filter(r -> !r.getCreatedAt().isBefore(from) && r.getCreatedAt().isBefore(to))
                .toList();

        Map<RecoveryPath, Long> counts = recommendations.stream()
                .collect(Collectors.groupingBy(RecoveryRecommendation::getRecommendedPath, Collectors.counting()));
        Map<RecoveryPath, BigDecimal> values = recommendations.stream()
                .collect(Collectors.groupingBy(RecoveryRecommendation::getRecommendedPath,
                        Collectors.reducing(BigDecimal.ZERO,
                                r -> r.getRecommendedNetValue() == null ? BigDecimal.ZERO : r.getRecommendedNetValue(),
                                BigDecimal::add)));

        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        BigDecimal totalValue = values.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        List<DistributionSlice> slices = new ArrayList<>();
        for (RecoveryPath path : RecoveryPath.values()) {
            long count = counts.getOrDefault(path, 0L);
            BigDecimal value = values.getOrDefault(path, BigDecimal.ZERO);
            double percentage = total == 0 ? 0 : count * 100.0 / total;
            slices.add(new DistributionSlice(path.name(), path.getLabel(), count,
                    BigDecimal.valueOf(percentage).setScale(2, RoundingMode.HALF_UP),
                    value,
                    totalValue.signum() == 0 ? BigDecimal.ZERO
                            : value.multiply(BigDecimal.valueOf(100)).divide(totalValue, 2, RoundingMode.HALF_UP)));
        }
        return slices;
    }

    /**
     * Map markers for warehouses, partner facilities and scheduled pickups.
     */
    @Transactional(readOnly = true)
    public List<PickupMapMarker> pickupMarkers(UUID organizationId) {
        List<PickupMapMarker> markers = new ArrayList<>();
        List<PickupJob> pickups = pickupRepository.findByOrganizationIdAndStatusIn(organizationId,
                List.of(PickupStatus.SCHEDULED, PickupStatus.IN_TRANSIT, PickupStatus.COLLECTED,
                        PickupStatus.DELIVERED, PickupStatus.FAILED));

        for (PickupJob pickup : pickups) {
            if (pickup.getOriginLatitude() == null || pickup.getOriginLongitude() == null) {
                continue;
            }
            markers.add(new PickupMapMarker(
                    pickup.getId().toString(),
                    "PICKUP",
                    pickup.getOriginAddress() == null ? pickup.getPickupNumber() : pickup.getOriginAddress(),
                    pickup.getOriginLatitude().doubleValue(),
                    pickup.getOriginLongitude().doubleValue(),
                    pickup.getStatus().name(),
                    pickup.getStatus().getLabel(),
                    pickup.getScheduledDate() == null ? null : pickup.getScheduledDate().toString(),
                    pickup.getPickupNumber()));
        }
        return markers;
    }

    /**
     * A full report for the analytics screen.
     */
    @Transactional(readOnly = true)
    public ReportSummary report(UUID organizationId, LocalDate from, LocalDate to) {
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<ReturnRequest> returns = returnRepository.findCreatedBetween(organizationId, fromInstant, toInstant);

        long total = returns.size();
        long recovered = returns.stream()
                .filter(r -> r.getStatus() == ReturnStatus.RECOVERED || r.getStatus() == ReturnStatus.COMPLETED)
                .count();
        BigDecimal estimated = returns.stream()
                .map(ReturnRequest::getEstimatedRecoveryValue)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal realized = returns.stream()
                .map(ReturnRequest::getRealizedRecoveryValue)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Double turnaround = averageTurnaroundDays(returns);

        List<PartnerPerformance> partnerPerformance = jobRepository
                .findByOrganizationIdAndStatusIn(organizationId, List.of(JobStatus.COMPLETED)).stream()
                .filter(job -> job.getPartnerName() != null)
                .collect(Collectors.groupingBy(RecoveryJob::getPartnerName, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new PartnerPerformance(entry.getKey(), entry.getValue(), null, null))
                .sorted(Comparator.comparingLong(PartnerPerformance::completedJobs).reversed())
                .toList();

        long consolidationSaved = pickupRepository
                .findByOrganizationIdAndStatusIn(organizationId,
                        List.of(PickupStatus.SCHEDULED, PickupStatus.IN_TRANSIT, PickupStatus.COLLECTED,
                                PickupStatus.DELIVERED)).stream()
                .filter(p -> p.getRouteId() != null)
                .count();

        return new ReportSummary(
                from.toString(),
                to.toString(),
                total,
                total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(recovered * 100.0 / total).setScale(2, RoundingMode.HALF_UP),
                estimated,
                realized,
                estimated.signum() == 0 ? BigDecimal.ZERO
                        : realized.multiply(BigDecimal.valueOf(100)).divide(estimated, 2, RoundingMode.HALF_UP),
                turnaround,
                recoveryDistribution(organizationId, fromInstant, toInstant),
                partnerPerformance,
                consolidationSaved,
                wasteAvoidedValue(returns),
                environmentalSummary(organizationId, fromInstant, toInstant));
    }

    /**
     * Environmental impact, stated as estimates with the methodology attached.
     */
    @Transactional(readOnly = true)
    public EnvironmentalSummary environmentalSummary(UUID organizationId, Instant from, Instant to) {
        List<RecoveryRecommendation> recommendations = recommendationRepository
                .findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .filter(r -> !r.getCreatedAt().isBefore(from) && r.getCreatedAt().isBefore(to))
                .toList();

        BigDecimal diverted = BigDecimal.ZERO;
        BigDecimal carbon = BigDecimal.ZERO;
        int items = 0;
        for (RecoveryRecommendation recommendation : recommendations) {
            items++;
            RecoveryOption option = findOption(recommendation);
            if (option == null) {
                continue;
            }
            if (option.wasteDivertedKg() != null) {
                diverted = diverted.add(option.wasteDivertedKg());
            }
            if (option.carbonAvoidedKg() != null) {
                carbon = carbon.add(option.carbonAvoidedKg());
            }
        }

        return new EnvironmentalSummary(
                diverted.setScale(2, RoundingMode.HALF_UP),
                carbon.setScale(2, RoundingMode.HALF_UP),
                items,
                List.of(
                        "Waste diverted is derived from the weight of recovered goods multiplied by the "
                                + "recovery-rate assumption for the selected path.",
                        "Carbon avoided is derived from diverted weight multiplied by a published "
                                + "embodied-carbon factor for the product category (electronics 8.5 kg CO2e/kg, "
                                + "generic goods 2.1 kg CO2e/kg).",
                        "Both figures are estimates based on documented assumptions, not measured emissions."));
    }

    private RecoveryOption findOption(RecoveryRecommendation recommendation) {
        String json = recommendation.getOptionsJson();
        if (json == null) {
            return null;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            List<RecoveryOption> options = mapper.readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<>() {
                    });
            return options.stream()
                    .filter(o -> o.path() == recommendation.getRecommendedPath())
                    .findFirst()
                    .orElse(null);
        } catch (Exception ex) {
            return null;
        }
    }

    private BigDecimal wasteAvoidedValue(List<ReturnRequest> returns) {
        return returns.stream()
                .map(ReturnRequest::getEstimatedRecoveryValue)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Double averageTurnaroundDays(List<ReturnRequest> returns) {
        List<Double> durations = returns.stream()
                .filter(r -> r.getStatusChangedAt() != null && r.getCreatedAt() != null)
                .map(r -> (double) Duration.between(r.getCreatedAt(), r.getStatusChangedAt()).toHours() / 24.0)
                .toList();
        if (durations.isEmpty()) {
            return null;
        }
        return durations.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    public record KpiCard(String key, String label, double value, Double previousValue, String description) {
    }

    public record TrendPoint(String date, long returnsCreated, long itemsRecovered) {
    }

    public record DistributionSlice(String path, String label, long count, BigDecimal percentage,
                                    BigDecimal value, BigDecimal valueShare) {
    }

    public record PickupMapMarker(String id, String type, String name, double latitude, double longitude,
                                  String status, String statusLabel, String scheduledDate, String reference) {
    }

    public record ReturnRow(UUID id, String returnNumber, String sku, String productName, String customerName,
                            ReturnStatus status, String statusLabel, BigDecimal estimatedRecoveryValue,
                            Instant createdAt) {
        static ReturnRow from(ReturnRequest request) {
            return new ReturnRow(request.getId(), request.getReturnNumber(), null, null,
                    request.getCustomerName(), request.getStatus(), request.getStatus().getLabel(),
                    request.getEstimatedRecoveryValue(), request.getCreatedAt());
        }
    }

    public record DashboardSummary(String from, String to, KpiCard totalReturns, KpiCard awaitingInspection,
                                   KpiCard recoveredValue, KpiCard wasteAvoided, KpiCard averageTurnaround,
                                   List<TrendPoint> trend, List<DistributionSlice> distribution,
                                   List<PickupMapMarker> markers, List<ReturnRow> recentReturns) {
    }

    public record PartnerPerformance(String partnerName, long completedJobs, BigDecimal onTimeRate,
                                     BigDecimal averageProcessingDays) {
    }

    public record EnvironmentalSummary(BigDecimal wasteDivertedKg, BigDecimal carbonAvoidedKg, int itemsAssessed,
                                       List<String> methodology) {
    }

    public record ReportSummary(String from, String to, long totalReturns, BigDecimal recoverySuccessRate,
                                BigDecimal estimatedValue, BigDecimal realizedValue,
                                BigDecimal realizationRate, Double averageTurnaroundDays,
                                List<DistributionSlice> recoveryDistribution,
                                List<PartnerPerformance> partnerPerformance, long consolidatedPickups,
                                BigDecimal wasteAvoidedValue, EnvironmentalSummary environmental) {
    }
}
