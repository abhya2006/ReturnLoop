package com.returnloop.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    @Query("""
            select a from AuditEvent a
            where a.organizationId = :org
              and (:action is null or a.action = :action)
              and (:entityType is null or a.entityType = :entityType)
            """)
    Page<AuditEvent> search(@Param("org") UUID organizationId,
                            @Param("action") String action,
                            @Param("entityType") String entityType,
                            Pageable pageable);
}
