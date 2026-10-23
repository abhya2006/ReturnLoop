package com.returnloop.logistics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * A capacity-constrained nearest-neighbour solver with a 2-opt improvement pass.
 *
 * <p>The algorithm:
 * <ol>
 *   <li>sorts pickups by priority (highest first) then by distance from the depot;</li>
 *   <li>assigns each pickup to the vehicle that is already carrying the most
 *       work while capacity allows, so collections are consolidated rather
 *       than spread across the fleet;</li>
 *   <li>orders each vehicle's stops with a nearest-neighbour sweep from the depot;</li>
 *   <li>improves the ordering with a 2-opt pass that reverses segments when
 *       doing so shortens the total distance;</li>
 *   <li>returns the route to the depot.</li>
 * </ol>
 *
 * <p>All distances are great-circle estimates from {@link GeoDistanceService};
 * they are not live navigation distances.
 */
@Component
@RequiredArgsConstructor
public class NearestNeighborRouteOptimizer implements RouteOptimizer {

    private static final int MAX_2OPT_PASSES = 12;

    private final GeoDistanceService geo;

    @Override
    public List<RoutePlan> plan(Depot depot, List<PickupJob> pickups, List<Vehicle> vehicles) {
        if (pickups == null || pickups.isEmpty() || vehicles == null || vehicles.isEmpty()) {
            return List.of();
        }

        List<PickupJob> ordered = new ArrayList<>(pickups);
        ordered.sort(Comparator
                .comparingInt((PickupJob job) -> -job.getPriority())
                .thenComparingDouble(job -> geo.distanceKm(
                        depot.lat(), depot.lon(),
                        job.getOriginLatitude(), job.getOriginLongitude())));

        List<List<PickupJob>> assignments = new ArrayList<>();
        List<Integer> usedUnits = new ArrayList<>();
        List<Double> usedWeight = new ArrayList<>();
        for (int i = 0; i < vehicles.size(); i++) {
            assignments.add(new ArrayList<>());
            usedUnits.add(0);
            usedWeight.add(0.0);
        }

        for (PickupJob job : ordered) {
            int chosen = -1;
            for (int i = 0; i < vehicles.size(); i++) {
                Vehicle vehicle = vehicles.get(i);
                int units = Math.max(job.getUnitCount(), 0);
                double weight = job.getTotalWeightKg() == null ? 0 : job.getTotalWeightKg().doubleValue();

                boolean unitCapacityOk = usedUnits.get(i) + units <= vehicle.getCapacityUnits();
                boolean weightCapacityOk = vehicle.getCapacityKg() == null
                        || usedWeight.get(i) + weight <= vehicle.getCapacityKg().doubleValue();
                if (!unitCapacityOk || !weightCapacityOk) {
                    continue;
                }
                if (chosen == -1 || usedUnits.get(i) > usedUnits.get(chosen)
                        || (usedUnits.get(i) == usedUnits.get(chosen) && usedWeight.get(i) > usedWeight.get(chosen))) {
                    chosen = i;
                }
            }
            if (chosen == -1) {
                // Nothing has spare capacity: use the least loaded vehicle and
                // let the caller know the route is over capacity.
                chosen = 0;
                for (int i = 1; i < vehicles.size(); i++) {
                    if (usedUnits.get(i) < usedUnits.get(chosen)) {
                        chosen = i;
                    }
                }
            }
            assignments.get(chosen).add(job);
            usedUnits.set(chosen, usedUnits.get(chosen) + Math.max(job.getUnitCount(), 0));
            usedWeight.set(chosen, usedWeight.get(chosen)
                    + (job.getTotalWeightKg() == null ? 0 : job.getTotalWeightKg().doubleValue()));
        }

        List<RoutePlan> plans = new ArrayList<>();
        for (int i = 0; i < vehicles.size(); i++) {
            List<PickupJob> stops = assignments.get(i);
            if (stops.isEmpty()) {
                continue;
            }
            List<PickupJob> sequenced = sequence(depot, stops);
            double distance = measure(depot, sequenced);
            int duration = geo.estimatedDurationMinutes(distance);
            double weight = sequenced.stream()
                    .mapToDouble(j -> j.getTotalWeightKg() == null ? 0 : j.getTotalWeightKg().doubleValue())
                    .sum();
            int units = sequenced.stream().mapToInt(PickupJob::getUnitCount).sum();
            Vehicle vehicle = vehicles.get(i);
            boolean capacityRespected = units <= vehicle.getCapacityUnits()
                    && (vehicle.getCapacityKg() == null
                    || weight <= vehicle.getCapacityKg().doubleValue());

            List<String> notes = new ArrayList<>();
            if (!capacityRespected) {
                notes.add("Load exceeds the capacity of " + vehicle.getName()
                        + "; split the route or add a vehicle");
            }
            notes.add("Estimated great-circle distance, not live navigation data");

            plans.add(new RoutePlan(vehicle, sequenced, distance, duration, weight, capacityRespected, notes));
        }
        return plans;
    }

    /**
     * Nearest-neighbour construction followed by a 2-opt improvement pass.
     */
    private List<PickupJob> sequence(Depot depot, List<PickupJob> stops) {
        List<PickupJob> remaining = new ArrayList<>(stops);
        List<PickupJob> result = new ArrayList<>();

        double currentLat = depot.lat();
        double currentLon = depot.lon();
        while (!remaining.isEmpty()) {
            PickupJob nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            for (PickupJob job : remaining) {
                double distance = geo.distanceKm(currentLat, currentLon,
                        job.getOriginLatitude(), job.getOriginLongitude());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = job;
                }
            }
            result.add(nearest);
            remaining.remove(nearest);
            currentLat = nearest.getOriginLatitude() == null ? currentLat : nearest.getOriginLatitude().doubleValue();
            currentLon = nearest.getOriginLongitude() == null ? currentLon : nearest.getOriginLongitude().doubleValue();
        }

        return twoOpt(depot, result);
    }

    /**
     * Reverses segments of the tour while that reduces the total distance.
     */
    private List<PickupJob> twoOpt(Depot depot, List<PickupJob> tour) {
        if (tour.size() < 4) {
            return tour;
        }
        List<PickupJob> best = new ArrayList<>(tour);
        double bestDistance = measure(depot, best);
        boolean improved = true;
        int pass = 0;

        while (improved && pass < MAX_2OPT_PASSES) {
            improved = false;
            pass++;
            for (int i = 0; i < best.size() - 1; i++) {
                for (int j = i + 1; j < best.size(); j++) {
                    List<PickupJob> candidate = new ArrayList<>(best);
                    Collections.reverse(candidate.subList(i, j + 1));
                    double distance = measure(depot, candidate);
                    if (distance + 1e-9 < bestDistance) {
                        best = candidate;
                        bestDistance = distance;
                        improved = true;
                    }
                }
            }
        }
        return best;
    }

    private double measure(Depot depot, List<PickupJob> tour) {
        double total = 0;
        double lat = depot.lat();
        double lon = depot.lon();
        for (PickupJob job : tour) {
            total += geo.distanceKm(lat, lon, job.getOriginLatitude(), job.getOriginLongitude());
            lat = job.getOriginLatitude() == null ? lat : job.getOriginLatitude().doubleValue();
            lon = job.getOriginLongitude() == null ? lon : job.getOriginLongitude().doubleValue();
        }
        if (!tour.isEmpty()) {
            total += geo.distanceKm(lat, lon, depot.lat(), depot.lon());
        }
        return total;
    }

    @Override
    public String algorithmName() {
        return "nearest-neighbor-2opt";
    }

    /**
     * Converts a plan into persisted route stops.
     */
    public List<RouteStop> toStops(Depot depot, RoutePlan plan, UUID routeId) {
        List<RouteStop> stops = new ArrayList<>();
        double lat = depot.lat();
        double lon = depot.lon();
        int sequence = 1;
        for (PickupJob job : plan.orderedPickups()) {
            RouteStop stop = new RouteStop();
            stop.setOrganizationId(job.getOrganizationId());
            stop.setRouteId(routeId);
            stop.setPickupJobId(job.getId());
            stop.setSequence(sequence++);
            stop.setStopType("PICKUP");
            stop.setLocationName(job.getOriginAddress() == null ? job.getPickupNumber() : job.getOriginAddress());
            stop.setAddress(job.getOriginAddress());
            stop.setLatitude(job.getOriginLatitude());
            stop.setLongitude(job.getOriginLongitude());
            stop.setUnitCount(job.getUnitCount());
            double leg = geo.distanceKm(lat, lon, job.getOriginLatitude(), job.getOriginLongitude());
            stop.setLegDistanceKm(BigDecimal.valueOf(leg).setScale(3, RoundingMode.HALF_UP));
            stop.setLegDurationMinutes(geo.estimatedDurationMinutes(leg));
            stops.add(stop);
            lat = job.getOriginLatitude() == null ? lat : job.getOriginLatitude().doubleValue();
            lon = job.getOriginLongitude() == null ? lon : job.getOriginLongitude().doubleValue();
        }
        return stops;
    }
}
