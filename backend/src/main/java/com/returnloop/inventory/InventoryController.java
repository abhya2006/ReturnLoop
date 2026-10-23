package com.returnloop.inventory;

import com.returnloop.common.web.ApiResponse;
import com.returnloop.common.web.PageResponse;
import com.returnloop.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Returned and unsold stock")
public class InventoryController {

    private final InventoryService service;
    private final CurrentUser currentUser;

    @GetMapping
    public PageResponse<InventoryItemResponse> search(@RequestParam(required = false) String search,
                                                      @RequestParam(required = false) InventoryStatus status,
                                                      @RequestParam(required = false) String category,
                                                      @RequestParam(required = false) UUID warehouseId,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "25") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Order.desc("createdAt")));
        return service.search(currentUser.organizationId(), search, status, category, warehouseId, pageable);
    }

    @GetMapping("/{id}")
    public ApiResponse<InventoryItemDetail> get(@PathVariable UUID id) {
        return ApiResponse.of(service.get(currentUser.organizationId(), id));
    }

    @GetMapping("/aging")
    @Operation(summary = "Items held beyond the configured holding threshold")
    public ApiResponse<List<InventoryService.AgingItem>> aging(
            @RequestParam(defaultValue = "30") int thresholdDays) {
        return ApiResponse.of(service.aging(currentUser.organizationId(), thresholdDays));
    }

    @PostMapping("/receive")
    @Operation(summary = "Book goods into a warehouse")
    public ApiResponse<InventoryItemResponse> receive(@RequestBody InventoryService.ReceiveRequest request) {
        var principal = currentUser.require();
        return ApiResponse.of(service.receive(principal.organizationId(), request, principal.fullName()));
    }

    @PostMapping("/{id}/reserve")
    public ApiResponse<InventoryItemResponse> reserve(@PathVariable UUID id,
                                                      @RequestBody QuantityRequest request) {
        var principal = currentUser.require();
        return ApiResponse.of(service.reserve(principal.organizationId(), id, request.quantity(),
                request.referenceId(), principal.fullName()));
    }

    @PostMapping("/{id}/release")
    public ApiResponse<InventoryItemResponse> release(@PathVariable UUID id,
                                                      @RequestBody QuantityRequest request) {
        var principal = currentUser.require();
        return ApiResponse.of(service.release(principal.organizationId(), id, request.quantity(),
                request.referenceId(), principal.fullName()));
    }

    @PostMapping("/{id}/dispatch")
    public ApiResponse<InventoryItemResponse> dispatch(@PathVariable UUID id,
                                                       @RequestBody QuantityRequest request) {
        var principal = currentUser.require();
        return ApiResponse.of(service.dispatch(principal.organizationId(), id, request.quantity(),
                request.referenceId(), principal.fullName()));
    }

    @PostMapping("/{id}/adjust")
    public ApiResponse<InventoryItemResponse> adjust(@PathVariable UUID id,
                                                      @RequestBody AdjustRequest request) {
        var principal = currentUser.require();
        return ApiResponse.of(service.adjust(principal.organizationId(), id, request.delta(),
                request.reason(), principal.fullName()));
    }

    public record QuantityRequest(int quantity, UUID referenceId) {
    }

    public record AdjustRequest(int delta, String reason) {
    }
}
