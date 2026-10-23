package com.returnloop.returns;

import com.returnloop.catalog.Product;
import com.returnloop.catalog.ProductRepository;
import com.returnloop.common.error.ApiException;
import com.returnloop.common.error.ErrorCode;
import com.returnloop.common.event.DomainEvent;
import com.returnloop.common.event.DomainEventPublisher;
import com.returnloop.common.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Owns the return lifecycle: creation, item management, transitions, and the
 * timeline of lifecycle events.
 */
@Service
@RequiredArgsConstructor
public class ReturnService {

    private final ReturnRequestRepository returnRepository;
    private final ReturnItemRepository itemRepository;
    private final ReturnEventRepository eventRepository;
    private final ProductRepository productRepository;
    private final DomainEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PageResponse<ReturnSummary> search(UUID organizationId, String search, List<ReturnStatus> statuses,
                                               ReturnReason reason, Instant from, Instant to, Pageable pageable) {
        Page<ReturnRequest> page = returnRepository.search(organizationId,
                search == null || search.isBlank() ? null : search.trim(),
                statuses == null || statuses.isEmpty() ? null : statuses,
                reason, from, to, pageable);
        return PageResponse.from(page, ReturnSummary::from);
    }

    @Transactional(readOnly = true)
    public ReturnDetail get(UUID organizationId, UUID returnId) {
        ReturnRequest request = require(organizationId, returnId);
        List<ReturnItem> items = itemRepository.findByReturnIdOrderById(returnId);
        List<ReturnEvent> events = eventRepository.findByReturnIdOrderByCreatedAtAsc(returnId);
        return ReturnDetail.from(request, items, events);
    }

    @Transactional
    public ReturnDetail create(UUID organizationId, String actor, CreateReturnRequest request) {
        List<CreateReturnItem> items = request.items();
        if (items == null || items.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "A return must contain at least one item");
        }

        ReturnRequest entity = new ReturnRequest();
        entity.setOrganizationId(organizationId);
        entity.setReturnNumber(nextReturnNumber(organizationId));
        entity.setStatus(ReturnStatus.RETURN_CREATED);
        entity.setReason(request.reason() == null ? ReturnReason.OTHER : request.reason());
        entity.setSourceReference(request.sourceReference());
        entity.setCustomerName(request.customerName());
        entity.setCustomerEmail(request.customerEmail());
        entity.setOriginAddress(request.originAddress());
        entity.setOriginLatitude(request.originLatitude());
        entity.setOriginLongitude(request.originLongitude());
        entity.setWarehouseId(request.warehouseId());
        entity.setNotes(request.notes());
        entity.setStatusChangedAt(Instant.now());
        ReturnRequest saved = returnRepository.save(entity);

        BigDecimal estimatedTotal = BigDecimal.ZERO;
        int totalQuantity = 0;
        for (CreateReturnItem item : items) {
            Product product = productRepository.findByOrganizationIdAndSkuIgnoreCase(organizationId, item.sku())
                    .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND,
                            "No product is registered with SKU " + item.sku()));
            if (item.quantity() == null || item.quantity() < 1) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Item quantity must be at least 1");
            }
            ReturnItem entityItem = new ReturnItem();
            entityItem.setOrganizationId(organizationId);
            entityItem.setReturnId(saved.getId());
            entityItem.setProductId(product.getId());
            entityItem.setSku(product.getSku());
            entityItem.setProductName(product.getName());
            entityItem.setQuantity(item.quantity());
            entityItem.setCondition(item.condition() == null ? ProductCondition.GOOD : item.condition());
            entityItem.setSerialNumber(item.serialNumber());
            entityItem.setAccessoriesComplete(item.accessoriesComplete() == null || item.accessoriesComplete());
            BigDecimal unitValue = product.getResaleValue() == null ? BigDecimal.ZERO : product.getResaleValue();
            BigDecimal lineValue = unitValue.multiply(BigDecimal.valueOf(item.quantity()));
            entityItem.setEstimatedValue(lineValue);
            entityItem.setNotes(item.notes());
            itemRepository.save(entityItem);

            estimatedTotal = estimatedTotal.add(lineValue);
            totalQuantity += item.quantity();
        }

        saved.setItemCount(totalQuantity);
        saved.setEstimatedRecoveryValue(estimatedTotal);
        returnRepository.save(saved);

        recordEvent(saved, "RETURN_CREATED", null, ReturnStatus.RETURN_CREATED.name(),
                "Return created with " + totalQuantity + " item(s)", actor);
        eventPublisher.publish(DomainEvent.of("return.created", organizationId, "ReturnRequest", saved.getId(),
                actor, Map.of("returnNumber", saved.getReturnNumber(), "itemCount", totalQuantity)));

        return get(organizationId, saved.getId());
    }

    @Transactional
    public ReturnDetail addItem(UUID organizationId, UUID returnId, String actor, CreateReturnItem item) {
        ReturnRequest request = require(organizationId, returnId);
        if (request.getStatus().isTerminal()) {
            throw ApiException.invalidState("A " + request.getStatus() + " return can no longer be modified");
        }
        Product product = productRepository.findByOrganizationIdAndSkuIgnoreCase(organizationId, item.sku())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND,
                        "No product is registered with SKU " + item.sku()));

        ReturnItem entityItem = new ReturnItem();
        entityItem.setOrganizationId(organizationId);
        entityItem.setReturnId(returnId);
        entityItem.setProductId(product.getId());
        entityItem.setSku(product.getSku());
        entityItem.setProductName(product.getName());
        if (item.quantity() == null || item.quantity() < 1 || item.quantity() > 999) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Item quantity must be between 1 and 999");
        }
        entityItem.setQuantity(item.quantity());
        entityItem.setCondition(item.condition() == null ? ProductCondition.GOOD : item.condition());
        entityItem.setSerialNumber(item.serialNumber());
        entityItem.setAccessoriesComplete(item.accessoriesComplete() == null || item.accessoriesComplete());
        BigDecimal unitValue = product.getResaleValue() == null ? BigDecimal.ZERO : product.getResaleValue();
        entityItem.setEstimatedValue(unitValue.multiply(BigDecimal.valueOf(entityItem.getQuantity())));
        itemRepository.save(entityItem);

        request.setItemCount(request.getItemCount() + entityItem.getQuantity());
        request.setEstimatedRecoveryValue(
                (request.getEstimatedRecoveryValue() == null ? BigDecimal.ZERO : request.getEstimatedRecoveryValue())
                        .add(entityItem.getEstimatedValue()));
        returnRepository.save(request);

        recordEvent(request, "ITEM_ADDED", request.getStatus().name(), request.getStatus().name(),
                "Item " + entityItem.getSku() + " added", actor);
        return get(organizationId, returnId);
    }

    /**
     * Moves the return to a new lifecycle status, validating the transition.
     */
    @Transactional
    public ReturnDetail transition(UUID organizationId, UUID returnId, ReturnStatus target, String actor,
                                    String reason) {
        ReturnRequest request = require(organizationId, returnId);
        ReturnStatus current = request.getStatus();
        if (current == target) {
            return get(organizationId, returnId);
        }
        if (!current.canTransitionTo(target)) {
            throw ApiException.invalidState("Cannot move a return from " + current + " to " + target
                    + ". Allowed next steps: " + current.allowedTransitions());
        }
        if (target == ReturnStatus.CANCELLED && (reason == null || reason.isBlank())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "A cancellation reason is required");
        }

        request.setStatus(target);
        request.setStatusChangedAt(Instant.now());
        if (target == ReturnStatus.CANCELLED) {
            request.setCancelledReason(reason);
        }
        returnRepository.save(request);

        recordEvent(request, "STATUS_CHANGED", current.name(), target.name(),
                reason == null || reason.isBlank() ? "Status changed to " + target.getLabel() : reason, actor);
        eventPublisher.publish(DomainEvent.of("return.status-changed", organizationId, "ReturnRequest", returnId,
                actor, Map.of("from", current.name(), "to", target.name())));

        return get(organizationId, returnId);
    }

    @Transactional
    public ReturnDetail cancel(UUID organizationId, UUID returnId, String actor, String reason) {
        return transition(organizationId, returnId, ReturnStatus.CANCELLED, actor, reason);
    }

    @Transactional
    public ReturnDetail assignWarehouse(UUID organizationId, UUID returnId, String actor, UUID warehouseId) {
        ReturnRequest request = require(organizationId, returnId);
        request.setWarehouseId(warehouseId);
        returnRepository.save(request);
        recordEvent(request, "WAREHOUSE_ASSIGNED", request.getStatus().name(), request.getStatus().name(),
                "Warehouse assigned", actor);
        return get(organizationId, returnId);
    }

    @Transactional
    public ReturnDetail setEstimatedValue(UUID organizationId, UUID returnId, String actor, BigDecimal value) {
        ReturnRequest request = require(organizationId, returnId);
        request.setEstimatedRecoveryValue(value);
        returnRepository.save(request);
        recordEvent(request, "VALUE_UPDATED", request.getStatus().name(), request.getStatus().name(),
                "Estimated recovery value set to " + value, actor);
        return get(organizationId, returnId);
    }

    @Transactional
    public ReturnDetail setRealizedValue(UUID organizationId, UUID returnId, String actor, BigDecimal value) {
        ReturnRequest request = require(organizationId, returnId);
        request.setRealizedRecoveryValue(value);
        returnRepository.save(request);
        recordEvent(request, "VALUE_REALISED", request.getStatus().name(), request.getStatus().name(),
                "Realised recovery value set to " + value, actor);
        return get(organizationId, returnId);
    }

    @Transactional(readOnly = true)
    public ReturnRequest require(UUID organizationId, UUID returnId) {
        ReturnRequest request = returnRepository.findById(returnId)
                .orElseThrow(() -> ApiException.notFound("Return", returnId));
        if (!request.getOrganizationId().equals(organizationId)) {
            throw new ApiException(ErrorCode.CROSS_TENANT_ACCESS);
        }
        return request;
    }

    @Transactional
    public void recordEvent(ReturnRequest request, String type, String from, String to, String description,
                            String actor) {
        ReturnEvent event = new ReturnEvent();
        event.setOrganizationId(request.getOrganizationId());
        event.setReturnId(request.getId());
        event.setEventType(type);
        event.setFromStatus(from);
        event.setToStatus(to);
        event.setDescription(description);
        event.setActor(actor);
        eventRepository.save(event);
    }

    /**
     * Generates a sequential, human readable return number for the organisation.
     */
    private String nextReturnNumber(UUID organizationId) {
        long sequence = returnRepository.countByOrganizationId(organizationId) + 1;
        String year = String.valueOf(LocalDate.now(ZoneOffset.UTC).getYear());
        return "RET-" + year + "-" + String.format("%05d", sequence);
    }
}
