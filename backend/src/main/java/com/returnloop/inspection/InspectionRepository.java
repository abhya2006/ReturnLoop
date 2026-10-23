package com.returnloop.inspection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InspectionRepository extends JpaRepository<Inspection, UUID> {

    Optional<Inspection> findFirstByReturnItemIdOrderByCreatedAtDesc(UUID returnItemId);

    List<Inspection> findByReturnIdOrderByCreatedAtAsc(UUID returnId);

    @Query("""
            select i from Inspection i
            where i.organizationId = :org
              and (:status is null or i.status = :status)
              and (:returnId is null or i.returnId = :returnId)
            """)
    Page<Inspection> search(@Param("org") UUID organizationId,
                             @Param("status") InspectionStatus status,
                             @Param("returnId") UUID returnId,
                             Pageable pageable);

    long countByOrganizationIdAndStatus(UUID organizationId, InspectionStatus status);
}
