package com.returnloop.logistics;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RouteRepository extends JpaRepository<Route, UUID> {

    @Query("""
            select r from Route r
            where r.organizationId = :org
              and (:status is null or r.status = :status)
              and (:date is null or r.routeDate = :date)
            """)
    Page<Route> search(@Param("org") UUID organizationId,
                       @Param("status") RouteStatus status,
                       @Param("date") LocalDate date,
                       Pageable pageable);

    List<Route> findByOrganizationIdOrderByRouteDateDescCreatedAtDesc(UUID organizationId);

    List<Route> findByOrganizationIdAndRouteDate(UUID organizationId, LocalDate routeDate);

    long countByOrganizationId(UUID organizationId);
}
