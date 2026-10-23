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
@RequestMapping("/api/v1/partner-jobs")
@RequiredArgsConstructor
@Tag(name = "Partner Jobs", description = "Recovery job lifecycle")
public class PartnerJobController {

    private final PartnerService service;
    private final CurrentUser currentUser;

    @GetMapping
    public PageResponse<RecoveryJobSummary> list(@RequestParam(required = false) String search,
                                                 @RequestParam(required = false) List<JobStatus> status,
                                                 @RequestParam(required = false) UUID partnerId,
                                                 @RequestParam(required = false) RecoveryPath path,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "25") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Order.desc("createdAt")));
        return service.listJobs(currentUser.organizationId(), search, status, partnerId, path, pageable);
    }

    @GetMapping("/{id}")
    public ApiResponse<RecoveryJobDetail> get(@PathVariable UUID id) {
        return ApiResponse.of(service.getJob(currentUser.organizationId(), id));
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "Advance a job through the partner lifecycle")
    public ApiResponse<RecoveryJobDetail> transition(@PathVariable UUID id,
                                                     @RequestBody StatusRequest request) {
        var principal = currentUser.require();
        return ApiResponse.of(service.transition(currentUser.organizationId(), id, request.status(),
                request.note(), request.realizedValue(), principal.fullName()));
    }

    public record StatusRequest(JobStatus status, String note, BigDecimal realizedValue) {
    }
}
