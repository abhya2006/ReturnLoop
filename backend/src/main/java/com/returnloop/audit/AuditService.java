package com.returnloop.audit;

import com.returnloop.common.web.PageResponse;
import com.returnloop.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Records and serves the audit trail.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository repository;
    private final CurrentUser currentUser;

    @Transactional
    public void record(UUID organizationId, String action, String entityType, UUID entityId, String summary) {
        AuditEvent event = new AuditEvent();
        event.setOrganizationId(organizationId);
        event.setAction(action);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setSummary(summary);
        currentUser.get().ifPresent(user -> {
            event.setActor(user.fullName());
            event.setActorRole(user.role().name());
        });
        repository.save(event);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEventResponse> search(UUID organizationId, String action, String entityType,
                                                   Pageable pageable) {
        Page<AuditEvent> page = repository.search(organizationId, action, entityType, pageable);
        return PageResponse.from(page, AuditEventResponse::from);
    }
}
