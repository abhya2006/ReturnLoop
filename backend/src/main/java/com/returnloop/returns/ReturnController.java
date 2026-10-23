package com.returnloop.returns;

import com.returnloop.common.idempotency.IdempotencyService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/returns")
@RequiredArgsConstructor
@Tag(name = "Returns", description = "Return request lifecycle")
public class ReturnController {

    private final ReturnService service;
    private final CurrentUser currentUser;
    private final IdempotencyService idempotencyService;

    @GetMapping
    @Operation(summary = "Search returns")
    public PageResponse<ReturnSummary> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<ReturnStatus> status,
            @RequestParam(required = false) ReturnReason reason,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(parseSort(sort)));
        return service.search(currentUser.organizationId(), search, status, reason, from, to, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get return detail with items and lifecycle timeline")
    public ApiResponse<ReturnDetail> get(@PathVariable UUID id) {
        return ApiResponse.of(service.get(currentUser.organizationId(), id));
    }

    @PostMapping
    @Operation(summary = "Create a return with one or more items")
    public ResponseEntity<ApiResponse<ReturnDetail>> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateReturnRequest request) {
        var principal = currentUser.require();
        Optional<ReturnDetail> replay = idempotencyService.findReplay(
                principal.organizationId(), idempotencyKey, "create-return", request, ReturnDetail.class);
        if (replay.isPresent()) {
            return ResponseEntity.ok(ApiResponse.of(replay.get()));
        }
        ReturnDetail created = service.create(principal.organizationId(), principal.fullName(), request);
        idempotencyService.remember(principal.organizationId(), idempotencyKey, "create-return", request, created, 201);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created));
    }

    @PostMapping("/{id}/items")
    @Operation(summary = "Add an item to an existing return")
    public ApiResponse<ReturnDetail> addItem(@PathVariable UUID id,
                                             @Valid @RequestBody CreateReturnItem item) {
        var principal = currentUser.require();
        return ApiResponse.of(service.addItem(principal.organizationId(), id, principal.fullName(), item));
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "Advance the return to the next lifecycle status")
    public ApiResponse<ReturnDetail> transition(@PathVariable UUID id,
                                                @RequestBody TransitionRequest request) {
        var principal = currentUser.require();
        return ApiResponse.of(service.transition(principal.organizationId(), id, request.status(),
                principal.fullName(), request.reason()));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an eligible return, recording the reason")
    public ApiResponse<ReturnDetail> cancel(@PathVariable UUID id,
                                            @RequestBody CancelRequest request) {
        var principal = currentUser.require();
        return ApiResponse.of(service.cancel(principal.organizationId(), id, principal.fullName(), request.reason()));
    }

    @PostMapping("/{id}/warehouse")
    @Operation(summary = "Assign the receiving warehouse")
    public ApiResponse<ReturnDetail> assignWarehouse(@PathVariable UUID id,
                                                     @RequestBody AssignWarehouseRequest request) {
        var principal = currentUser.require();
        return ApiResponse.of(service.assignWarehouse(principal.organizationId(), id, principal.fullName(),
                request.warehouseId()));
    }

    @PostMapping("/{id}/estimated-value")
    public ApiResponse<ReturnDetail> setEstimatedValue(@PathVariable UUID id,
                                                      @RequestBody ValueRequest request) {
        var principal = currentUser.require();
        return ApiResponse.of(service.setEstimatedValue(principal.organizationId(), id, principal.fullName(),
                request.value()));
    }

    @PostMapping("/{id}/realized-value")
    public ApiResponse<ReturnDetail> setRealizedValue(@PathVariable UUID id,
                                                      @RequestBody ValueRequest request) {
        var principal = currentUser.require();
        return ApiResponse.of(service.setRealizedValue(principal.organizationId(), id, principal.fullName(),
                request.value()));
    }

    private static Sort.Order[] parseSort(String sort) {
        String[] parts = sort.split(",");
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        String property = parts[0].isBlank() ? "createdAt" : parts[0];
        return new Sort.Order[]{Sort.Order.by(property).with(direction)};
    }

    public record TransitionRequest(ReturnStatus status, String reason) {
    }

    public record CancelRequest(String reason) {
    }

    public record AssignWarehouseRequest(UUID warehouseId) {
    }

    public record ValueRequest(BigDecimal value) {
    }
}
