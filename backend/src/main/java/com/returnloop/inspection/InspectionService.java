package com.returnloop.inspection;

import com.returnloop.common.error.ApiException;
import com.returnloop.common.error.ErrorCode;
import com.returnloop.common.event.DomainEvent;
import com.returnloop.common.event.DomainEventPublisher;
import com.returnloop.common.web.PageResponse;
import com.returnloop.returns.ProductCondition;
import com.returnloop.returns.ReturnItem;
import com.returnloop.returns.ReturnItemRepository;
import com.returnloop.returns.ReturnRequest;
import com.returnloop.returns.ReturnService;
import com.returnloop.returns.ReturnStatus;
import com.returnloop.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Warehouse inspection workflow. Completed inspections are immutable.
 */
@Service
@RequiredArgsConstructor
public class InspectionService {

    private final InspectionRepository inspectionRepository;
    private final InspectionAttachmentRepository attachmentRepository;
    private final ReturnItemRepository returnItemRepository;
    private final ReturnService returnService;
    private final DomainEventPublisher eventPublisher;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public PageResponse<InspectionResponse> search(UUID organizationId, InspectionStatus status, UUID returnId,
                                                   Pageable pageable) {
        Page<Inspection> page = inspectionRepository.search(organizationId, status, returnId, pageable);
        return PageResponse.from(page, this::toResponse);
    }

    @Transactional(readOnly = true)
    public InspectionDetail get(UUID organizationId, UUID inspectionId) {
        Inspection inspection = require(organizationId, inspectionId);
        List<InspectionAttachment> attachments = attachmentRepository
                .findByInspectionIdOrderByCreatedAtAsc(inspectionId);
        return InspectionDetail.from(inspection, attachments);
    }

    /**
     * Opens an inspection for a return item. One open inspection per item.
     */
    @Transactional
    public InspectionDetail open(UUID organizationId, UUID returnItemId) {
        ReturnItem item = returnItemRepository.findById(returnItemId)
                .orElseThrow(() -> ApiException.notFound("Return item", returnItemId));
        if (!item.getOrganizationId().equals(organizationId)) {
            throw new ApiException(ErrorCode.CROSS_TENANT_ACCESS);
        }
        var existing = inspectionRepository.findFirstByReturnItemIdOrderByCreatedAtDesc(returnItemId);
        if (existing.isPresent() && existing.get().getStatus() != InspectionStatus.AWAITING_INSPECTION) {
            throw ApiException.conflict("An inspection already exists for this item");
        }

        Inspection inspection = existing.orElseGet(Inspection::new);
        inspection.setOrganizationId(organizationId);
        inspection.setReturnId(item.getReturnId());
        inspection.setReturnItemId(returnItemId);
        inspection.setSku(item.getSku());
        inspection.setProductName(item.getProductName());
        inspection.setStatus(InspectionStatus.AWAITING_INSPECTION);
        Inspection saved = inspectionRepository.save(inspection);

        if (existing.isEmpty()) {
            ReturnRequest request = returnService.require(organizationId, item.getReturnId());
            returnService.recordEvent(request, "INSPECTION_OPENED", request.getStatus().name(),
                    request.getStatus().name(), "Inspection opened for " + item.getSku(), actor());
        }
        return get(organizationId, saved.getId());
    }

    /**
     * Records inspection findings. Only open or in-progress inspections can be
     * edited; a completed inspection is locked.
     */
    @Transactional
    public InspectionDetail record(UUID organizationId, UUID inspectionId, RecordInspectionRequest request) {
        Inspection inspection = require(organizationId, inspectionId);
        if (inspection.getStatus().isFinal()) {
            throw ApiException.invalidState("This inspection is complete and can no longer be edited");
        }
        if (request.packagingCondition() == null && request.functionalityResult() == null
                && request.notes() == null && request.damageDescription() == null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "At least one inspection field is required");
        }

        if (request.packagingCondition() != null) {
            inspection.setPackagingCondition(request.packagingCondition());
        }
        if (request.functionalityResult() != null) {
            inspection.setFunctionalityResult(request.functionalityResult());
            inspection.setFunctional(request.functional() != null && request.functional());
        }
        if (request.accessoriesComplete() != null) {
            inspection.setAccessoriesComplete(request.accessoriesComplete());
        }
        if (request.damageDescription() != null) {
            inspection.setDamageDescription(request.damageDescription());
        }
        if (request.missingAccessories() != null) {
            inspection.setMissingAccessories(request.missingAccessories());
        }
        if (request.estimatedRepairCost() != null) {
            inspection.setEstimatedRepairCost(request.estimatedRepairCost());
        }
        if (request.notes() != null) {
            inspection.setNotes(request.notes());
        }
        if (request.status() != null) {
            inspection.setStatus(request.status());
        }
        if (request.flaggedUnsafe() != null) {
            inspection.setFlaggedUnsafe(request.flaggedUnsafe());
        }
        inspectionRepository.save(inspection);

        return get(organizationId, inspectionId);
    }

    /**
     * Completes the inspection, locks it and advances the return lifecycle.
     */
    @Transactional
    public InspectionDetail complete(UUID organizationId, UUID inspectionId) {
        Inspection inspection = require(organizationId, inspectionId);
        if (inspection.getStatus().isFinal()) {
            throw ApiException.invalidState("This inspection is already complete");
        }
        if (inspection.getFunctionalityResult() == null && inspection.getPackagingCondition() == null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Record a packaging condition and a functionality result before completing the inspection");
        }
        if (inspection.isFlaggedUnsafe()) {
            inspection.setStatus(InspectionStatus.UNSAFE);
        } else if (inspection.getStatus() == InspectionStatus.IN_PROGRESS
                || inspection.getStatus() == InspectionStatus.AWAITING_INSPECTION) {
            inspection.setStatus(inspection.isFunctional() ? InspectionStatus.PASSED : InspectionStatus.DAMAGED);
        }
        inspection.setInspectedBy(actor());
        inspection.setInspectedAt(Instant.now());
        inspectionRepository.save(inspection);

        ReturnRequest request = returnService.require(organizationId, inspection.getReturnId());
        returnService.recordEvent(request, "INSPECTION_COMPLETED", request.getStatus().name(),
                request.getStatus().name(),
                "Inspection completed for " + inspection.getSku() + " with result " + inspection.getStatus().name(),
                actor());

        if (request.getStatus() == ReturnStatus.WAREHOUSE_RECEIVED) {
            returnService.transition(organizationId, request.getId(), ReturnStatus.INSPECTED, actor(),
                    "Inspection completed");
        }

        eventPublisher.publish(DomainEvent.of("inspection.completed", organizationId, "ReturnRequest",
                inspection.getReturnId(), actor(),
                Map.of("inspectionId", inspection.getId().toString(),
                        "status", inspection.getStatus().name(),
                        "sku", inspection.getSku() == null ? "" : inspection.getSku())));

        return get(organizationId, inspectionId);
    }

    @Transactional
    public InspectionDetail addAttachment(UUID organizationId, UUID inspectionId, String fileName,
                                           String contentType, long sizeBytes, String caption) {
        Inspection inspection = require(organizationId, inspectionId);
        if (inspection.getStatus().isFinal()) {
            throw ApiException.invalidState("A completed inspection cannot accept new attachments");
        }
        if (sizeBytes <= 0) {
            throw new ApiException(ErrorCode.FILE_REJECTED, "The uploaded file is empty");
        }
        InspectionAttachment attachment = new InspectionAttachment();
        attachment.setOrganizationId(organizationId);
        attachment.setInspectionId(inspectionId);
        attachment.setFileName(fileName);
        attachment.setContentType(contentType);
        attachment.setStoragePath("inspection/" + inspectionId + "/" + fileName);
        attachment.setSizeBytes(sizeBytes);
        attachment.setCaption(caption);
        attachmentRepository.save(attachment);
        return get(organizationId, inspectionId);
    }

    @Transactional(readOnly = true)
    public Inspection require(UUID organizationId, UUID inspectionId) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> ApiException.notFound("Inspection", inspectionId));
        if (!inspection.getOrganizationId().equals(organizationId)) {
            throw new ApiException(ErrorCode.CROSS_TENANT_ACCESS);
        }
        return inspection;
    }

    private InspectionResponse toResponse(Inspection inspection) {
        return InspectionResponse.from(inspection);
    }

    private String actor() {
        return currentUser.get().map(u -> u.fullName()).orElse("system");
    }

    public record RecordInspectionRequest(ProductCondition packagingCondition,
                                           ProductCondition functionalityResult,
                                           Boolean functional,
                                           Boolean accessoriesComplete,
                                           String damageDescription,
                                           String missingAccessories,
                                           BigDecimal estimatedRepairCost,
                                           String notes,
                                           InspectionStatus status,
                                           Boolean flaggedUnsafe) {
    }
}
