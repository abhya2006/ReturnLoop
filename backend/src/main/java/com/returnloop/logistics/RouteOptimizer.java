package com.returnloop.logistics;

import java.util.List;

/**
 * A vehicle-routing solver.
 *
 * <p>The interface exists so a more advanced solver (for example an
 * MILP-based approach) can be introduced later without touching the logistics
 * service or the API.
 */
public interface RouteOptimizer {

    /**
     * Groups pickup jobs into routes that respect vehicle capacity.
     *
     * @param depot   the depot the routes start and end at
     * @param pickups the pickups to serve
     * @param vehicles the available vehicles
     * @return one plan per vehicle
     */
    List<RoutePlan> plan(Depot depot, List<PickupJob> pickups, List<Vehicle> vehicles);

    /** Identifier of the algorithm, surfaced in the API so operators know what was used. */
    String algorithmName();

    /**
     * Turns a plan into the ordered stops that make up the route.
     */
    List<RouteStop> toStops(Depot depot, RoutePlan plan, java.util.UUID routeId);

    /**
     * A planned route for one vehicle.
     */
    record RoutePlan(Vehicle vehicle, List<PickupJob> orderedPickups, double totalDistanceKm,
                     int totalDurationMinutes, double totalWeightKg, boolean capacityRespected,
                     List<String> notes) {
    }
}
