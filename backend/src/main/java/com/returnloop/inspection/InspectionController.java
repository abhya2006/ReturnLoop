package com.returnloop.inspection;

import com.returnloop.common.web.ApiResponse;
import com.returnloop.common.web.PageResponse;
import com.returnloop.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inspections")
@RequiredArgsConstructor
@Tag(name = "Inspections", description = "Warehouse inspection workspace")
public class InspectionController {

    private final InspectionService service;
    private final CurrentUser currentUser;

    @GetMapping
    public PageResponse<InspectionResponse> search(@RequestParam(required = false) InspectionStatus status,
                                                   @RequestParam(required = false) UUID returnId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "25") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Order.desc("createdAt")));
        return service.search(currentUser.organizationId(), status, returnId, pageable);
    }

    @GetMapping("/{id}")
    public ApiResponse<InspectionDetail> get(@PathVariable UUID id) {
        return ApiResponse.of(service.get(currentUser.organizationId(), id));
    }

    @PostMapping("/items/{returnItemId}")
    @Operation(summary = "Open an inspection for a return item")
    public ApiResponse<InspectionDetail> open(@PathVariable UUID returnItemId) {
        return ApiResponse.of(service.open(currentUser.organizationId(), returnItemId));
    }

    @PostMapping("/{id}/record")
    @Operation(summary = "Record inspection findings")
    public ApiResponse<InspectionDetail> record(@PathVariable UUID id,
                                                @RequestBody InspectionService.RecordInspectionRequest request) {
        return ApiResponse.of(service.record(currentUser.organizationId(), id, request));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete the inspection and lock it")
    public ApiResponse<InspectionDetail> complete(@PathVariable UUID id) {
        return ApiResponse.of(service.complete(currentUser.organizationId(), id));
    }

    @PostMapping("/{id}/attachments")
    @Operation(summary = "Attach a photo or document to an inspection")
    public ApiResponse<InspectionDetail> addAttachment(@PathVariable UUID id,
                                                        @RequestBody AttachmentRequest request) {
        return ApiResponse.of(service.addAttachment(currentUser.organizationId(), id, request.fileName(),
                request.contentType(), request.sizeBytes(), request.caption()));
    }

    public record AttachmentRequest(String fileName, String contentType, long sizeBytes, String caption) {
    }
}
