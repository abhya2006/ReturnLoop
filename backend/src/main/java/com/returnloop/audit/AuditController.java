package com.returnloop.audit;

import com.returnloop.common.web.PageResponse;
import com.returnloop.security.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Audit trail")
public class AuditController {

    private final AuditService service;
    private final CurrentUser currentUser;

    @GetMapping
    public PageResponse<AuditEventResponse> search(@RequestParam(required = false) String action,
                                                   @RequestParam(required = false) String entityType,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Order.desc("createdAt")));
        return service.search(currentUser.organizationId(), action, entityType, pageable);
    }
}
