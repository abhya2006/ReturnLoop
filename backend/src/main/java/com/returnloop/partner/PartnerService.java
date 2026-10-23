package com.returnloop.partner;

import com.returnloop.common.error.ApiException;
import com.returnloop.common.error.ErrorCode;
import com.returnloop.common.event.DomainEvent;
import com.returnloop.common.event.DomainEventPublisher;
import com.returnloop.common.web.PageResponse;
import com.returnloop.recovery.RecoveryPath;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Partner discovery and job assignment.
 */
@Service
@RequiredArgsConstructor
public class PartnerService {

    /** Job statuses that count as "already assigned" for duplicate protection. */
    private static final List<JobStatus> ACTIVE_JOB_STATUSES = List.of(
            JobStatus.PROPOSED, JobStatus.ASSIGNED, JobStatus.ACCEPTED, JobStatus.PROCESSING,
            JobStatus.AWAITING_REVIEW, JobStatus.COMPLETED);

    private final PartnerRepository partnerRepository;
    private final RecoveryJobRepository jobRepository;
    private final RecoveryJobEventRepository jobEventRepository;
    private final DomainEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PageResponse<PartnerResponse> search(UUID organizationId, String search, PartnerType type,
                                                String category, String region, boolean availableOnly,
                                                Pageable pageable) {
        Page<Partner> page = partnerRepository.search(organizationId,
                search == null || search.isBlank() ? null : search.trim(), type, category, region,
                availableOnly, pageable);
        return PageResponse.from(page, PartnerResponse::from);
    }

    @Transactional(readOnly = true)
    public PartnerDetail get(UUID organizationId, UUID partnerId) {
        Partner partner = require(organizationId, partnerId);
        List<RecoveryJob> history = jobRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId).stream()
                .filter(job -> job.getOrganizationId().equals(organizationId))
                .limit(25)
                .toList();
        return PartnerDetail.from(partner, history);
    }

    @Transactional(readOnly = true)
    public Partner require(UUID organizationId, UUID partnerId) {
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> ApiException.notFound("Partner", partnerId));
        if (!partner.getOrganizationId().equals(organizationId)) {
            throw new ApiException(ErrorCode.CROSS_TENANT_ACCESS);
        }
        return partner;
    }

    /**
     * Finds partners that can perform a given recovery path, ranked by
     * availability, capacity and recorded performance.
     */
    @Transactional(readOnly = true)
    public List<PartnerMatch> match(UUID organizationId, RecoveryPath path, String category, String region) {
        PartnerType type = partnerTypeFor(path);
        List<Partner> candidates = new ArrayList<>(
                partnerRepository.findByOrganizationIdAndPartnerTypeAndCurrentlyAvailableTrue(organizationId, type));
        if (candidates.isEmpty()) {
            candidates = new ArrayList<>(partnerRepository.search(organizationId, null, type, category, region,
                    false, Pageable.unpaged()).getContent());
        }

        return candidates.stream()
                .filter(partner -> supportsCategory(partner, category))
                .filter(partner -> servesRegion(partner, region))
                .map(partner -> new PartnerMatch(
                        partner.getId(),
                        partner.getName(),
                        partner.getPartnerType().name(),
                        partner.isCurrentlyAvailable(),
                        partner.getProcessingCapacityPerDay(),
                        partner.getFeePerUnit(),
                        partner.getFeePercentage(),
                        partner.getAverageProcessingDays(),
                        partner.getOnTimeRate(),
                        partner.getAcceptanceRate(),
                        partner.getCompletedJobs(),
                        partner.isVerified(),
                        matchReason(partner, path, category, region)))
                .sorted((a, b) -> Double.compare(matchScore(b), matchScore(a)))
                .toList();
    }

    /**
     * Creates a recovery job and assigns it to a partner.
     */
    @Transactional
    public RecoveryJobDetail assign(UUID organizationId, UUID inventoryItemId, AssignJobRequest request,
                                     String actor) {
        UUID partnerId = request.partnerId();
        Partner partner = require(organizationId, partnerId);
        if (!partner.isCurrentlyAvailable()) {
            throw new ApiException(ErrorCode.INVALID_STATE_TRANSITION,
                    partner.getName() + " is not currently accepting work");
        }
        if (request.inventoryItemId() != null) {
            boolean duplicate = jobRepository
                    .findActiveForInventoryItem(organizationId, request.inventoryItemId(), ACTIVE_JOB_STATUSES)
                    .stream()
                    .anyMatch(job -> !job.getPartnerId().equals(partnerId));
            if (duplicate) {
                throw new ApiException(ErrorCode.DUPLICATE_ASSIGNMENT,
                        "These goods already have an active recovery job with another partner");
            }
        }

        RecoveryJob job = new RecoveryJob();
        job.setOrganizationId(organizationId);
        job.setJobNumber(nextJobNumber(organizationId));
        job.setReturnId(request.returnId());
        job.setReturnItemId(request.returnItemId());
        job.setInventoryItemId(inventoryItemId);
        job.setSku(request.sku());
        job.setProductName(request.productName());
        job.setQuantity(request.quantity() == null ? 1 : Math.max(request.quantity(), 1));
        job.setRecoveryPath(request.recoveryPath());
        job.setStatus(JobStatus.ASSIGNED);
        job.setPartnerId(partnerId);
        job.setPartnerName(partner.getName());
        job.setExpectedRecoveryValue(request.expectedRecoveryValue());
        job.setCostEstimate(estimateCost(partner, request.expectedRecoveryValue(), job.getQuantity()));
        job.setServiceDeadline(request.serviceDeadline());
        job.setAssignedAt(Instant.now());
        RecoveryJob saved = jobRepository.save(job);

        recordEvent(saved, "ASSIGNED", null, JobStatus.ASSIGNED.name(),
                "Job assigned to " + partner.getName(), actor);
        eventPublisher.publish(DomainEvent.of("partner.job-assigned", organizationId, "RecoveryJob", saved.getId(),
                actor, Map.of("partner", partner.getName(), "path", request.recoveryPath().name())));

        return getJob(organizationId, saved.getId());
    }

    /**
     * Advances a job through its lifecycle, enforcing valid transitions.
     */
    @Transactional
    public RecoveryJobDetail transition(UUID organizationId, UUID jobId, JobStatus target, String note,
                                         BigDecimal realizedValue, String actor) {
        RecoveryJob job = requireJob(organizationId, jobId);
        JobStatus current = job.getStatus();
        if (current == target) {
            return getJob(organizationId, jobId);
        }
        if (!current.canTransitionTo(target)) {
            throw ApiException.invalidState("Cannot move a job from " + current + " to " + target
                    + ". Allowed next steps: " + current.allowedTransitions());
        }
        if (target == JobStatus.REJECTED && (note == null || note.isBlank())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "A rejection reason is required");
        }

        job.setStatus(target);
        switch (target) {
            case ACCEPTED -> job.setAcceptedAt(Instant.now());
            case PROCESSING -> job.setStartedAt(Instant.now());
            case COMPLETED -> {
                job.setCompletedAt(Instant.now());
                if (realizedValue != null) {
                    job.setRealizedValue(realizedValue);
                }
            }
            case REJECTED -> job.setRejectionReason(note);
            case FAILED -> job.setExceptionNote(note);
            default -> {
            }
        }
        if (note != null && !note.isBlank() && target != JobStatus.REJECTED) {
            job.setProgressNote(note);
        }
        jobRepository.save(job);

        recordEvent(job, "STATUS_CHANGED", current.name(), target.name(),
                note == null || note.isBlank() ? "Status changed to " + target.getLabel() : note, actor);

        if (target == JobStatus.COMPLETED) {
            updatePartnerMetrics(job);
        }
        eventPublisher.publish(DomainEvent.of("partner.job-status-changed", organizationId, "RecoveryJob", jobId,
                actor, Map.of("from", current.name(), "to", target.name())));

        return getJob(organizationId, jobId);
    }

    @Transactional(readOnly = true)
    public RecoveryJobDetail getJob(UUID organizationId, UUID jobId) {
        RecoveryJob job = requireJob(organizationId, jobId);
        List<RecoveryJobEvent> events = jobEventRepository.findByJobIdOrderByCreatedAtAsc(jobId);
        return RecoveryJobDetail.from(job, events);
    }

    @Transactional(readOnly = true)
    public PageResponse<RecoveryJobSummary> listJobs(UUID organizationId, String search, List<JobStatus> statuses,
                                                       UUID partnerId, RecoveryPath path, Pageable pageable) {
        Page<RecoveryJob> page = jobRepository.search(organizationId,
                search == null || search.isBlank() ? null : search.trim(),
                statuses == null || statuses.isEmpty() ? null : statuses, partnerId, path, pageable);
        return PageResponse.from(page, RecoveryJobSummary::from);
    }

    @Transactional(readOnly = true)
    public RecoveryJob requireJob(UUID organizationId, UUID jobId) {
        RecoveryJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> ApiException.notFound("Recovery job", jobId));
        if (!job.getOrganizationId().equals(organizationId)) {
            throw new ApiException(ErrorCode.CROSS_TENANT_ACCESS);
        }
        return job;
    }

    private void recordEvent(RecoveryJob job, String type, String from, String to, String description, String actor) {
        RecoveryJobEvent event = new RecoveryJobEvent();
        event.setOrganizationId(job.getOrganizationId());
        event.setJobId(job.getId());
        event.setEventType(type);
        event.setFromStatus(from);
        event.setToStatus(to);
        event.setDescription(description);
        event.setActor(actor);
        jobEventRepository.save(event);
    }

    /**
     * Recomputes a partner's performance metrics from completed jobs. The
     * metrics are always derived, never entered by hand.
     */
    private void updatePartnerMetrics(RecoveryJob job) {
        if (job.getPartnerId() == null) {
            return;
        }
        Partner partner = partnerRepository.findById(job.getPartnerId()).orElse(null);
        if (partner == null) {
            return;
        }
        List<RecoveryJob> jobs = jobRepository.findByPartnerIdOrderByCreatedAtDesc(partner.getId());
        long completed = jobs.stream().filter(j -> j.getStatus() == JobStatus.COMPLETED).count();
        long failed = jobs.stream().filter(j -> j.getStatus() == JobStatus.FAILED).count();
        long decided = jobs.stream()
                .filter(j -> j.getStatus() == JobStatus.ACCEPTED || j.getStatus() == JobStatus.REJECTED)
                .count();
        long accepted = jobs.stream().filter(j -> j.getStatus() == JobStatus.ACCEPTED).count();
        long onTime = jobs.stream()
                .filter(j -> j.getStatus() == JobStatus.COMPLETED
                        && j.getServiceDeadline() != null
                        && j.getCompletedAt() != null
                        && j.getCompletedAt().isBefore(j.getServiceDeadline().atStartOfDay(java.time.ZoneOffset.UTC).toInstant()))
                .count();

        partner.setCompletedJobs((int) completed);
        partner.setFailedJobs((int) failed);
        partner.setAcceptanceRate(decided == 0 ? null
                : BigDecimal.valueOf(accepted * 100.0 / decided).setScale(2, RoundingMode.HALF_UP));
        partner.setOnTimeRate(completed == 0 ? null
                : BigDecimal.valueOf(onTime * 100.0 / completed).setScale(2, RoundingMode.HALF_UP));

        List<RecoveryJob> completedJobs = jobs.stream().filter(j -> j.getStatus() == JobStatus.COMPLETED).toList();
        if (!completedJobs.isEmpty()) {
            double totalDays = completedJobs.stream()
                    .filter(j -> j.getCompletedAt() != null && j.getAcceptedAt() != null)
                    .mapToDouble(j -> Duration.between(j.getAcceptedAt(), j.getCompletedAt()).toDays())
                    .sum();
            long measured = completedJobs.stream()
                    .filter(j -> j.getCompletedAt() != null && j.getAcceptedAt() != null).count();
            partner.setAverageProcessingDays(measured == 0 ? null
                    : BigDecimal.valueOf(totalDays / measured).setScale(2, RoundingMode.HALF_UP));
        }
        partnerRepository.save(partner);
    }

    private BigDecimal estimateCost(Partner partner, BigDecimal expectedValue, int quantity) {
        BigDecimal fee = BigDecimal.ZERO;
        if (partner.getFeePerUnit() != null) {
            fee = partner.getFeePerUnit().multiply(BigDecimal.valueOf(quantity));
        } else if (partner.getFeePercentage() != null && expectedValue != null) {
            fee = expectedValue.multiply(partner.getFeePercentage()
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        }
        return fee.setScale(2, RoundingMode.HALF_UP);
    }

    private String nextJobNumber(UUID organizationId) {
        long sequence = jobRepository.countByOrganizationId(organizationId) + 1;
        return "JOB-" + String.format("%05d", sequence);
    }

    private PartnerType partnerTypeFor(RecoveryPath path) {
        return switch (path) {
            case REPAIR -> PartnerType.REPAIR;
            case REFURBISHMENT -> PartnerType.REFURBISHMENT;
            case RESTOCK_AS_NEW, OPEN_BOX_RESALE -> PartnerType.RESALE;
            case LIQUIDATION -> PartnerType.LIQUIDATION;
            case SPARE_PARTS, RECYCLING, SAFE_DISPOSAL -> PartnerType.RECYCLING;
        };
    }

    private boolean supportsCategory(Partner partner, String category) {
        if (category == null || category.isBlank()) {
            return true;
        }
        String supported = partner.getSupportedCategories();
        if (supported == null || supported.isBlank()) {
            return true;
        }
        return Arrays.stream(supported.split(","))
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .anyMatch(s -> s.equals(category.toUpperCase(Locale.ROOT)));
    }

    private boolean servesRegion(Partner partner, String region) {
        if (region == null || region.isBlank()) {
            return true;
        }
        String regions = partner.getServiceableRegions();
        if (regions == null || regions.isBlank()) {
            return true;
        }
        return Arrays.stream(regions.split(","))
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .anyMatch(s -> s.equals(region.toUpperCase(Locale.ROOT)));
    }

    private String matchReason(Partner partner, RecoveryPath path, String category, String region) {
        StringBuilder builder = new StringBuilder("Handles ").append(path.getLabel().toLowerCase(Locale.ROOT));
        if (category != null && !category.isBlank()) {
            builder.append(" for ").append(category.toLowerCase(Locale.ROOT));
        }
        if (region != null && !region.isBlank()) {
            builder.append(" in ").append(region);
        }
        if (partner.getAverageProcessingDays() != null) {
            builder.append(". Average turnaround ")
                    .append(partner.getAverageProcessingDays().stripTrailingZeros().toPlainString())
                    .append(" days");
        }
        return builder.append(".").toString();
    }

    private double matchScore(PartnerMatch match) {
        double score = 0;
        if (match.verified()) {
            score += 20;
        }
        if (match.onTimeRate() != null) {
            score += match.onTimeRate().doubleValue() * 0.3;
        }
        if (match.acceptanceRate() != null) {
            score += match.acceptanceRate().doubleValue() * 0.2;
        }
        if (match.averageProcessingDays() != null) {
            score += Math.max(0, 30 - match.averageProcessingDays().doubleValue());
        }
        score += Math.min(match.processingCapacityPerDay(), 200) * 0.05;
        return score;
    }

    public record AssignJobRequest(UUID inventoryItemId, UUID returnId, UUID returnItemId, String sku,
                                   String productName, Integer quantity, RecoveryPath recoveryPath,
                                   BigDecimal expectedRecoveryValue, java.time.LocalDate serviceDeadline,
                                   UUID partnerId) {
    }

    public record PartnerMatch(UUID id, String name, String partnerType, boolean available,
                               int processingCapacityPerDay, BigDecimal feePerUnit, BigDecimal feePercentage,
                               BigDecimal averageProcessingDays, BigDecimal onTimeRate, BigDecimal acceptanceRate,
                               int completedJobs, boolean verified, String matchReason) {
    }
}
