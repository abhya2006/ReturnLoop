package com.returnloop.returns;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, UUID> {

    Optional<ReturnRequest> findByOrganizationIdAndReturnNumber(UUID organizationId, String returnNumber);

    @Query("""
            select r from ReturnRequest r
            where r.organizationId = :org
              and (:search is null
                   or lower(r.returnNumber) like lower(concat('%', :search, '%'))
                   or lower(coalesce(r.customerName, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(r.sourceReference, '')) like lower(concat('%', :search, '%')))
              and (:statuses is null or r.status in :statuses)
              and (:reason is null or r.reason = :reason)
              and (:from is null or r.createdAt >= :from)
              and (:to is null or r.createdAt <= :to)
            """)
    Page<ReturnRequest> search(@Param("org") UUID organizationId,
                               @Param("search") String search,
                               @Param("statuses") List<ReturnStatus> statuses,
                               @Param("reason") ReturnReason reason,
                               @Param("from") Instant from,
                               @Param("to") Instant to,
                               Pageable pageable);

    long countByOrganizationIdAndStatusIn(UUID organizationId, List<ReturnStatus> statuses);

    long countByOrganizationId(UUID organizationId);

    List<ReturnRequest> findByOrganizationIdAndStatusIn(UUID organizationId, List<ReturnStatus> statuses);

    @Query("""
            select r from ReturnRequest r
            where r.organizationId = :org and r.createdAt >= :from and r.createdAt < :to
            order by r.createdAt asc
            """)
    List<ReturnRequest> findCreatedBetween(@Param("org") UUID organizationId,
                                            @Param("from") Instant from,
                                            @Param("to") Instant to);

    @Query("""
            select count(r) from ReturnRequest r
            where r.organizationId = :org and r.createdAt >= :from and r.createdAt < :to
            """)
    long countCreatedBetween(@Param("org") UUID organizationId,
                             @Param("from") Instant from,
                             @Param("to") Instant to);

    Optional<ReturnRequest> findFirstByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
