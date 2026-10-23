package com.returnloop.logistics;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PickupJobRepository extends JpaRepository<PickupJob, UUID> {

    @Query("""
            select p from PickupJob p
            where p.organizationId = :org
              and (:statuses is null or p.status in :statuses)
              and (:date is null or p.scheduledDate = :date)
              and (:vehicleId is null or p.vehicleId = :vehicleId)
            """)
    Page<PickupJob> search(@Param("org") UUID organizationId,
                           @Param("statuses") List<PickupStatus> statuses,
                           @Param("date") LocalDate date,
                           @Param("vehicleId") UUID vehicleId,
                           Pageable pageable);

    List<PickupJob> findByOrganizationIdAndStatusIn(UUID organizationId, List<PickupStatus> statuses);

    List<PickupJob> findByOrganizationIdAndScheduledDate(UUID organizationId, LocalDate date);

    List<PickupJob> findByRouteIdOrderByStopSequenceAsc(UUID routeId);

    Optional<PickupJob> findByOrganizationIdAndReturnId(UUID organizationId, UUID returnId);

    long countByOrganizationIdAndStatusIn(UUID organizationId, List<PickupStatus> statuses);

    long countByOrganizationId(UUID organizationId);
}
