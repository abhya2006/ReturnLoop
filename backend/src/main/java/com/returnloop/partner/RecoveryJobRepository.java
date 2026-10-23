package com.returnloop.partner;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecoveryJobRepository extends JpaRepository<RecoveryJob, UUID> {

    @Query("""
            select j from RecoveryJob j
            where j.organizationId = :org
              and (:search is null or lower(j.jobNumber) like lower(concat('%', :search, '%'))
                   or lower(j.sku) like lower(concat('%', :search, '%'))
                   or lower(coalesce(j.partnerName, '')) like lower(concat('%', :search, '%')))
              and (:statuses is null or j.status in :statuses)
              and (:partnerId is null or j.partnerId = :partnerId)
              and (:path is null or j.recoveryPath = :path)
            """)
    Page<RecoveryJob> search(@Param("org") UUID organizationId,
                             @Param("search") String search,
                             @Param("statuses") List<JobStatus> statuses,
                             @Param("partnerId") UUID partnerId,
                             @Param("path") com.returnloop.recovery.RecoveryPath path,
                             Pageable pageable);

    Optional<RecoveryJob> findByOrganizationIdAndJobNumber(UUID organizationId, String jobNumber);

    /**
     * Finds an active job for an inventory item, used to prevent duplicate
     * assignments for the same goods.
     */
    @Query("""
            select j from RecoveryJob j
            where j.organizationId = :org
              and j.inventoryItemId = :inventoryItemId
              and j.status in :statuses
            """)
    List<RecoveryJob> findActiveForInventoryItem(@Param("org") UUID organizationId,
                                                  @Param("inventoryItemId") UUID inventoryItemId,
                                                  @Param("statuses") List<JobStatus> statuses);

    List<RecoveryJob> findByOrganizationIdAndStatusIn(UUID organizationId, List<JobStatus> statuses);

    long countByOrganizationIdAndStatusIn(UUID organizationId, List<JobStatus> statuses);

    long countByOrganizationId(UUID organizationId);

    List<RecoveryJob> findByPartnerIdOrderByCreatedAtDesc(UUID partnerId);
}
