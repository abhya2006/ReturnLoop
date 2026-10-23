package com.returnloop.inventory;

import com.returnloop.common.error.ApiException;
import com.returnloop.common.error.ErrorCode;
import com.returnloop.common.web.PageResponse;
import com.returnloop.returns.ProductCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Inventory bookkeeping. All quantity changes go through this service so that
 * stock can never go negative and every change is recorded as a movement.
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository itemRepository;
    private final InventoryMovementRepository movementRepository;

    @Transactional(readOnly = true)
    public PageResponse<InventoryItemResponse> search(UUID organizationId, String search, InventoryStatus status,
                                                      String category, UUID warehouseId, Pageable pageable) {
        Page<InventoryItem> page = itemRepository.search(organizationId,
                search == null || search.isBlank() ? null : search.trim(),
                status, category, warehouseId, pageable);
        return PageResponse.from(page, InventoryItemResponse::from);
    }

    @Transactional(readOnly = true)
    public InventoryItemDetail get(UUID organizationId, UUID itemId) {
        InventoryItem item = require(organizationId, itemId);
        List<InventoryMovement> movements = movementRepository
                .findByInventoryItemIdOrderByCreatedAtDesc(itemId);
        return InventoryItemDetail.from(item, movements);
    }

    @Transactional
    public InventoryItemResponse receive(UUID organizationId, ReceiveRequest request, String actor) {
        if (request.quantity() == null || request.quantity() < 1) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Quantity must be at least 1");
        }
        InventoryItem item = itemRepository
                .findByOrganizationIdAndReturnIdAndSku(organizationId, request.returnId(), request.sku())
                .orElseGet(() -> {
                    InventoryItem created = new InventoryItem();
                    created.setOrganizationId(organizationId);
                    created.setReturnId(request.returnId());
                    created.setSku(request.sku());
                    created.setProductName(request.productName());
                    created.setCategory(request.category());
                    created.setCondition(request.condition() == null ? ProductCondition.GOOD : request.condition());
                    created.setWarehouseId(request.warehouseId());
                    created.setLocationCode(request.locationCode());
                    created.setUnitValue(request.unitValue());
                    created.setWeightKg(request.weightKg());
                    created.setStatus(InventoryStatus.AVAILABLE);
                    return created;
                });

        item.setProductName(request.productName() == null ? item.getProductName() : request.productName());
        item.setCategory(request.category() == null ? item.getCategory() : request.category());
        item.setCondition(request.condition() == null ? item.getCondition() : request.condition());
        item.setWarehouseId(request.warehouseId() == null ? item.getWarehouseId() : request.warehouseId());
        item.setLocationCode(request.locationCode() == null ? item.getLocationCode() : request.locationCode());
        item.setUnitValue(request.unitValue() == null ? item.getUnitValue() : request.unitValue());
        item.setWeightKg(request.weightKg() == null ? item.getWeightKg() : request.weightKg());
        item.setQuantity(item.getQuantity() + request.quantity());
        item.setEstimatedValue(unitValue(item).multiply(BigDecimal.valueOf(item.getQuantity())));
        item.setReceivedAt(Instant.now());
        item.setStatus(InventoryStatus.AVAILABLE);
        InventoryItem saved = itemRepository.save(item);

        recordMovement(saved, InventoryMovement.MovementType.RECEIPT, request.quantity(),
                null, saved.getStatus().name(), null, "return", request.returnId(), actor,
                "Goods received into stock");
        return InventoryItemResponse.from(saved);
    }

    /**
     * Reserves stock for a recovery job. Rejects over-allocation.
     */
    @Transactional
    public InventoryItemResponse reserve(UUID organizationId, UUID itemId, int quantity, UUID jobId, String actor) {
        InventoryItem item = require(organizationId, itemId);
        if (quantity < 1) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Reservation quantity must be at least 1");
        }
        int available = item.getQuantity() - item.getReservedQuantity();
        if (quantity > available) {
            throw new ApiException(ErrorCode.CAPACITY_EXCEEDED,
                    "Only " + available + " unit(s) are available to reserve");
        }
        InventoryStatus from = item.getStatus();
        item.setReservedQuantity(item.getReservedQuantity() + quantity);
        item.setStatus(InventoryStatus.RESERVED);
        item.setRecoveryJobId(jobId);
        InventoryItem saved = itemRepository.save(item);

        recordMovement(saved, InventoryMovement.MovementType.RESERVATION, quantity, from.name(),
                saved.getStatus().name(), null, "recovery-job", jobId, actor, "Stock reserved for recovery job");
        return InventoryItemResponse.from(saved);
    }

    @Transactional
    public InventoryItemResponse release(UUID organizationId, UUID itemId, int quantity, UUID jobId, String actor) {
        InventoryItem item = require(organizationId, itemId);
        if (quantity < 1 || quantity > item.getReservedQuantity()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Cannot release " + quantity + " unit(s); " + item.getReservedQuantity() + " are reserved");
        }
        InventoryStatus from = item.getStatus();
        item.setReservedQuantity(item.getReservedQuantity() - quantity);
        if (item.getReservedQuantity() == 0) {
            item.setStatus(InventoryStatus.AVAILABLE);
            item.setRecoveryJobId(null);
        }
        InventoryItem saved = itemRepository.save(item);
        recordMovement(saved, InventoryMovement.MovementType.RELEASE, quantity, from.name(),
                saved.getStatus().name(), null, "recovery-job", jobId, actor, "Reservation released");
        return InventoryItemResponse.from(saved);
    }

    @Transactional
    public InventoryItemResponse dispatch(UUID organizationId, UUID itemId, int quantity, UUID jobId, String actor) {
        InventoryItem item = require(organizationId, itemId);
        if (quantity < 1) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Dispatch quantity must be at least 1");
        }
        int allocated = item.getReservedQuantity() > 0 ? item.getReservedQuantity() : item.getQuantity();
        if (quantity > allocated) {
            throw new ApiException(ErrorCode.CAPACITY_EXCEEDED,
                    "Only " + allocated + " unit(s) are allocated for dispatch");
        }
        InventoryStatus from = item.getStatus();
        item.setQuantity(item.getQuantity() - quantity);
        item.setReservedQuantity(Math.max(0, item.getReservedQuantity() - quantity));
        item.setDispatchedAt(Instant.now());
        item.setStatus(item.getQuantity() == 0 ? InventoryStatus.DISPATCHED : InventoryStatus.AVAILABLE);
        item.setEstimatedValue(unitValue(item).multiply(BigDecimal.valueOf(item.getQuantity())));
        InventoryItem saved = itemRepository.save(item);
        recordMovement(saved, InventoryMovement.MovementType.DISPATCH, quantity, from.name(),
                saved.getStatus().name(), null, "recovery-job", jobId, actor, "Goods dispatched to partner");
        return InventoryItemResponse.from(saved);
    }

    @Transactional
    public InventoryItemResponse adjust(UUID organizationId, UUID itemId, int delta, String reason, String actor) {
        InventoryItem item = require(organizationId, itemId);
        int newQuantity = item.getQuantity() + delta;
        if (newQuantity < 0) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Adjustment would take stock below zero; current quantity is " + item.getQuantity());
        }
        if (item.getReservedQuantity() > newQuantity) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Adjustment would take stock below the reserved quantity");
        }
        InventoryStatus from = item.getStatus();
        item.setQuantity(newQuantity);
        item.setEstimatedValue(unitValue(item).multiply(BigDecimal.valueOf(newQuantity)));
        InventoryItem saved = itemRepository.save(item);
        recordMovement(saved, InventoryMovement.MovementType.ADJUSTMENT, delta, from.name(),
                saved.getStatus().name(), null, null, null, actor,
                reason == null ? "Stock adjusted" : reason);
        return InventoryItemResponse.from(saved);
    }

    /**
     * Lists items that have been held longer than the configured threshold.
     */
    @Transactional(readOnly = true)
    public List<AgingItem> aging(UUID organizationId, int thresholdDays) {
        Instant cutoff = Instant.now().minus(Duration.ofDays(Math.max(thresholdDays, 1)));
        List<InventoryStatus> statuses = List.of(InventoryStatus.AVAILABLE, InventoryStatus.RESERVED,
                InventoryStatus.DAMAGED, InventoryStatus.PROCESSING, InventoryStatus.QUARANTINED);
        return itemRepository.findAging(organizationId, statuses, cutoff).stream()
                .map(item -> new AgingItem(
                        item.getId(),
                        item.getSku(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getStatus().name(),
                        item.getEstimatedValue(),
                        item.getReceivedAt(),
                        Duration.between(item.getReceivedAt(), Instant.now()).toDays(),
                        thresholdDays))
                .toList();
    }

    @Transactional(readOnly = true)
    public InventoryItem require(UUID organizationId, UUID itemId) {
        InventoryItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> ApiException.notFound("Inventory item", itemId));
        if (!item.getOrganizationId().equals(organizationId)) {
            throw new ApiException(ErrorCode.CROSS_TENANT_ACCESS);
        }
        return item;
    }

    private BigDecimal unitValue(InventoryItem item) {
        if (item.getUnitValue() != null) {
            return item.getUnitValue();
        }
        if (item.getQuantity() > 0 && item.getEstimatedValue() != null) {
            return item.getEstimatedValue().divide(BigDecimal.valueOf(item.getQuantity()), 2,
                    java.math.RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private void recordMovement(InventoryItem item, InventoryMovement.MovementType type, int quantity,
                                String fromStatus, String toStatus, BigDecimal valueDelta, String referenceType,
                                UUID referenceId, String actor, String note) {
        InventoryMovement movement = new InventoryMovement();
        movement.setOrganizationId(item.getOrganizationId());
        movement.setInventoryItemId(item.getId());
        movement.setMovementType(type);
        movement.setQuantity(quantity);
        movement.setFromStatus(fromStatus);
        movement.setToStatus(toStatus);
        movement.setValueDelta(valueDelta);
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setActor(actor);
        movement.setNote(note);
        movementRepository.save(movement);
    }

    public record ReceiveRequest(UUID returnId, String sku, String productName, String category, Integer quantity,
                                 ProductCondition condition, UUID warehouseId, String locationCode,
                                 BigDecimal unitValue, BigDecimal weightKg) {
    }

    public record AgingItem(UUID id, String sku, String productName, int quantity, String status,
                            BigDecimal estimatedValue, Instant receivedAt, long daysHeld, int thresholdDays) {
    }
}
