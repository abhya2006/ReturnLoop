package com.returnloop.partner;

import com.returnloop.common.web.ApiResponse;
import com.returnloop.common.web.PageResponse;
import com.returnloop.recovery.RecoveryPath;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/partners")
@RequiredArgsConstructor
@Tag(name = "Partner Marketplace", description = "Partner discovery and job assignment")
public class PartnerController {

    private final PartnerService service;
    private final CurrentUser currentUser;

    @GetMapping
    public PageResponse<PartnerResponse> search(@RequestParam(required = false) String search,
                                                @RequestParam(required = false) PartnerType type,
                                                @RequestParam(required = false) String category,
                                                @RequestParam(required = false) String region,
                                                @RequestParam(defaultValue = "false") boolean availableOnly,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "25") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Order.asc("name")));
        return service.search(currentUser.organizationId(), search, type, category, region, availableOnly, pageable);
    }

    @GetMapping("/{id}")
    public ApiResponse<PartnerDetail> get(@PathVariable UUID id) {
        return ApiResponse.of(service.get(currentUser.organizationId(), id));
    }

    @GetMapping("/match")
    @Operation(summary = "Find partners able to perform a recovery path")
    public ApiResponse<List<PartnerService.PartnerMatch>> match(@RequestParam RecoveryPath path,
                                                                @RequestParam(required = false) String category,
                                                                @RequestParam(required = false) String region) {
        return ApiResponse.of(service.match(currentUser.organizationId(), path, category, region));
    }

    @PostMapping("/jobs")
    @Operation(summary = "Assign a recovery job to a partner")
    public ApiResponse<RecoveryJobDetail> assign(@RequestBody PartnerService.AssignJobRequest request) {
        var principal = currentUser.require();
        UUID inventoryItemId = request.inventoryItemId();
        return ApiResponse.of(service.assign(principal.organizationId(), inventoryItemId, request,
                principal.fullName()));
    }
}
