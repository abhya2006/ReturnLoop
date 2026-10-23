package com.returnloop.logistics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RouteStopRepository extends JpaRepository<RouteStop, UUID> {

    List<RouteStop> findByRouteIdOrderBySequenceAsc(UUID routeId);

    void deleteByRouteId(UUID routeId);
}
