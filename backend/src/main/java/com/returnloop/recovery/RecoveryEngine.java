package com.returnloop.recovery;

import com.returnloop.catalog.Product;
import com.returnloop.catalog.ProductRepository;
import com.returnloop.common.error.ApiException;
import com.returnloop.common.error.ErrorCode;
import com.returnloop.common.event.DomainEvent;
import com.returnloop.common.event.DomainEventPublisher;
import com.returnloop.inspection.Inspection;
import com.returnloop.inspection.InspectionRepository;
import com.returnloop.returns.ReturnItem;
import com.returnloop.returns.ReturnItemRepository;
import com.returnloop.returns.ReturnRequest;
import com.returnloop.returns.ReturnService;
import com.returnloop.returns.ReturnStatus;
import com.returnloop.security.CurrentUser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The recovery decision engine.
 *
 * <p>This is a rule-based, explainable system: it evaluates a documented cost
 * model against each candidate recovery path and ranks the feasible options.
 * It is deliberately <em>not</em> presented as a trained model. The
 * {@link RecoveryRankingStrategy} seam is where a learned model could be
 * introduced later without changing the rest of the application.
 */
@Service
@RequiredArgsConstructor
public class RecoveryEngine {

    private final RecoveryCostModel costModel = new RecoveryCostModel();
    private final RecoveryRankingStrategy rankingStrategy;
    private final RecoveryRecommendationRepository recommendationRepository;
    private final ReturnItemRepository returnItemRepository;
    private final ReturnService returnService;
    private final ProductRepository productRepository;
    private final InspectionRepository inspectionRepository;
    private final DomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final CurrentUser currentUser;

    /**
     * Evaluates the recovery options for a return without persisting anything.
     */
    @Transactional(readOnly = true)
    public RecoveryEvaluation preview(UUID organizationId, UUID returnId) {
        ReturnRequest request = returnService.require(organizationId, returnId);
        List<ReturnItem> items = returnItemRepository.findByReturnIdOrderById(returnId);
        if (items.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_STATE_TRANSITION,
                    "This return has no items to evaluate");
        }
        List<ItemEvaluation> evaluations = items.stream()
                .map(item -> evaluateItem(organizationId, request, item))
                .toList();
        return new RecoveryEvaluation(returnId, request.getReturnNumber(), rankingStrategy.name(),
                evaluations, Instant.now());
    }

    /**
     * Evaluates and persists a recovery recommendation for every item in a
     * return, and advances the return to {@code RECOVERY_DECISION}.
     */
    @Transactional
    public RecoveryEvaluation recommend(UUID organizationId, UUID returnId) {
        ReturnRequest request = returnService.require(organizationId, returnId);
        if (request.getStatus() != ReturnStatus.INSPECTED
                && request.getStatus() != ReturnStatus.RECOVERY_DECISION) {
            throw ApiException.invalidState("Recovery can only be recommended for an inspected return");
        }
        List<ReturnItem> items = returnItemRepository.findByReturnIdOrderById(returnId);
        if (items.isEmpty()) {
            throw ApiException.invalidState("This return has no items to evaluate");
        }

        // Validate the state transition before creating any recommendations.
        if (request.getStatus() == ReturnStatus.INSPECTED
                && !request.getStatus().canTransitionTo(ReturnStatus.RECOVERY_DECISION)) {
            throw ApiException.invalidState("This return cannot enter recovery decision");
        }

        String actor = currentUser.get().map(u -> u.fullName()).orElse("system");
        List<ItemEvaluation> evaluations = items.stream()
                .map(item -> {
                    ItemEvaluation evaluation = evaluateItem(organizationId, request, item);
                    persist(organizationId, request, item, evaluation);
                    return evaluation;
                })
                .toList();

        if (request.getStatus() != ReturnStatus.RECOVERY_DECISION) {
            returnService.transition(organizationId, returnId, ReturnStatus.RECOVERY_DECISION, actor,
                    "Recovery options calculated");
        }
        eventPublisher.publish(DomainEvent.of("recovery.recommended", organizationId, "ReturnRequest", returnId,
                actor, Map.of("itemCount", items.size(), "strategy", rankingStrategy.name())));

        return new RecoveryEvaluation(returnId, request.getReturnNumber(), rankingStrategy.name(),
                evaluations, Instant.now());
    }

    private ItemEvaluation evaluateItem(UUID organizationId, ReturnRequest request, ReturnItem item) {
        RecoveryContext context = buildContext(organizationId, item);
        Map<RecoveryPath, RecoveryOption> evaluated = costModel.evaluate(context);
        List<RecoveryOption> ranked = rankingStrategy.rank(List.copyOf(evaluated.values()), context);

        RecoveryOption recommended = ranked.stream()
                .filter(RecoveryOption::eligible)
                .findFirst()
                .orElse(ranked.isEmpty() ? null : ranked.get(0));

        return new ItemEvaluation(
                item.getId(),
                item.getSku(),
                item.getProductName(),
                item.getQuantity(),
                context.condition(),
                recommended,
                ranked,
                context.resaleValueOrZero(),
                new BigDecimal("0"));
    }

    private RecoveryContext buildContext(UUID organizationId, ReturnItem item) {
        Product product = productRepository.findById(item.getProductId()).orElse(null);
        Inspection inspection = inspectionRepository
                .findFirstByReturnItemIdOrderByCreatedAtDesc(item.getId())
                .orElse(null);

        boolean functional = inspection == null
                ? item.getCondition() != com.returnloop.returns.ProductCondition.DAMAGED
                && item.getCondition() != com.returnloop.returns.ProductCondition.UNSELLABLE
                && item.getCondition() != com.returnloop.returns.ProductCondition.UNSAFE
                : inspection.isFunctional();

        boolean accessoriesComplete = inspection == null
                ? item.isAccessoriesComplete()
                : inspection.isAccessoriesComplete();

        com.returnloop.returns.ProductCondition condition = inspection == null
                ? item.getCondition()
                : inspection.toCondition();

        BigDecimal resale = product != null && product.getResaleValue() != null
                ? product.getResaleValue()
                : item.getEstimatedValue();

        return new RecoveryContext(
                condition,
                resale,
                product == null ? null : product.getOriginalPrice(),
                product == null ? null : product.getWeightKg(),
                accessoriesComplete,
                functional,
                product == null ? null : product.getCategory().name(),
                requestRegion(item));
    }

    private String requestRegion(ReturnItem item) {
        return null;
    }

    private void persist(UUID organizationId, ReturnRequest request, ReturnItem item, ItemEvaluation evaluation) {
        RecoveryRecommendation entity = new RecoveryRecommendation();
        entity.setOrganizationId(organizationId);
        entity.setReturnId(request.getId());
        entity.setReturnItemId(item.getId());
        entity.setProductId(item.getProductId());
        entity.setSku(item.getSku());
        if (evaluation.recommended() == null) {
            throw ApiException.invalidState("No recovery option could be generated for item " + item.getSku());
        }
        entity.setRecommendedPath(evaluation.recommended().path());
        entity.setRecommendedNetValue(evaluation.recommended().netRecoveryValue());
        entity.setEstimatedProcessingDays(evaluation.recommended().estimatedProcessingDays());
        entity.setStrategy(rankingStrategy.name());
        try {
            entity.setOptionsJson(objectMapper.writeValueAsString(evaluation.options()));
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Unable to serialise recovery options", ex);
        }
        recommendationRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<RecoveryRecommendationResponse> listForReturn(UUID organizationId, UUID returnId) {
        return recommendationRepository.findByReturnIdOrderByCreatedAtDesc(returnId).stream()
                .filter(r -> r.getOrganizationId().equals(organizationId))
                .map(r -> toResponse(r, readOptions(r)))
                .toList();
    }

    @Transactional
    public RecoveryRecommendationResponse decide(UUID organizationId, UUID recommendationId, boolean approve,
                                                 String reason) {
        RecoveryRecommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> ApiException.notFound("Recovery recommendation", recommendationId));
        if (!recommendation.getOrganizationId().equals(organizationId)) {
            throw new ApiException(ErrorCode.CROSS_TENANT_ACCESS);
        }
        ReturnRequest returnRequest = returnService.require(organizationId, recommendation.getReturnId());
        if (returnRequest.getStatus() != ReturnStatus.RECOVERY_DECISION) {
            throw ApiException.invalidState("A recovery recommendation can only be decided while the return is in recovery decision");
        }
        if (recommendation.isApproved() || recommendation.isRejected()) {
            throw ApiException.invalidState("This recommendation has already been decided");
        }
        if (!approve && (reason == null || reason.isBlank())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "A reason is required when rejecting a recommendation");
        }
        var principal = currentUser.require();
        recommendation.setApproved(approve);
        recommendation.setRejected(!approve);
        recommendation.setDecisionReason(reason);
        recommendation.setDecidedBy(principal.fullName());
        recommendation.setDecidedAt(Instant.now());
        recommendationRepository.save(recommendation);

        eventPublisher.publish(DomainEvent.of("recovery.decision", organizationId, "ReturnRequest",
                recommendation.getReturnId(), principal.fullName(),
                Map.of("path", recommendation.getRecommendedPath().name(), "approved", approve)));

        return toResponse(recommendation, readOptions(recommendation));
    }

    private List<RecoveryOption> readOptions(RecoveryRecommendation recommendation) {
        if (recommendation.getOptionsJson() == null) {
            return List.of();
        }
        try {
            return objectMapper.readValue(recommendation.getOptionsJson(), new TypeReference<>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private RecoveryRecommendationResponse toResponse(RecoveryRecommendation entity, List<RecoveryOption> options) {
        return new RecoveryRecommendationResponse(
                entity.getId(),
                entity.getReturnId(),
                entity.getReturnItemId(),
                entity.getSku(),
                entity.getRecommendedPath(),
                entity.getRecommendedPath().getLabel(),
                entity.getRecommendedNetValue(),
                entity.getEstimatedProcessingDays(),
                entity.getStrategy(),
                entity.isApproved(),
                entity.isRejected(),
                entity.getDecisionReason(),
                entity.getDecidedBy(),
                entity.getDecidedAt(),
                options,
                entity.getCreatedAt());
    }

    public record RecoveryEvaluation(UUID returnId, String returnNumber, String strategy,
                                     List<ItemEvaluation> items, Instant generatedAt) {
    }

    public record ItemEvaluation(UUID returnItemId, String sku, String productName, int quantity,
                                  com.returnloop.returns.ProductCondition condition,
                                  RecoveryOption recommended, List<RecoveryOption> options,
                                  BigDecimal resaleValue, BigDecimal potentialValue) {
    }

    public record RecoveryRecommendationResponse(UUID id, UUID returnId, UUID returnItemId, String sku,
                                                 RecoveryPath recommendedPath, String recommendedPathLabel,
                                                 BigDecimal recommendedNetValue, Integer estimatedProcessingDays,
                                                 String strategy, boolean approved, boolean rejected,
                                                 String decisionReason, String decidedBy, Instant decidedAt,
                                                 List<RecoveryOption> options, Instant createdAt) {
    }
}
