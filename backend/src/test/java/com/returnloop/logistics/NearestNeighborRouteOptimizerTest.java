package com.returnloop.logistics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NearestNeighborRouteOptimizerTest {

    private final GeoDistanceService geo = new GeoDistanceService();
    private final NearestNeighborRouteOptimizer optimizer = new NearestNeighborRouteOptimizer(geo);

    private PickupJob pickup(String address, double lat, double lon, int units, int priority) {
        PickupJob job = new PickupJob();
        job.setId(UUID.randomUUID());
        job.setOrganizationId(UUID.randomUUID());
        job.setOriginAddress(address);
        job.setOriginLatitude(BigDecimal.valueOf(lat));
        job.setOriginLongitude(BigDecimal.valueOf(lon));
        job.setUnitCount(units);
        job.setPriority(priority);
        job.setTotalWeightKg(BigDecimal.valueOf(units * 2.0));
        return job;
    }

    private Vehicle vehicle(String name, int capacity) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(UUID.randomUUID());
        vehicle.setOrganizationId(UUID.randomUUID());
        vehicle.setName(name);
        vehicle.setCapacityUnits(capacity);
        return vehicle;
    }

    private Depot depot() {
        return new Depot(null, "Newark Hub", new BigDecimal("40.7357"), new BigDecimal("-74.1724"));
    }

    @Test
    @DisplayName("all pickups are assigned exactly once")
    void everyPickupIsAssigned() {
        PickupJob a = pickup("A", 40.80, -74.10, 2, 5);
        PickupJob b = pickup("B", 40.90, -74.00, 3, 5);
        PickupJob c = pickup("C", 41.10, -73.90, 1, 5);

        List<RouteOptimizer.RoutePlan> plans = optimizer.plan(depot(), List.of(a, b, c),
                List.of(vehicle("Van A", 100)));

        assertThat(plans).hasSize(1);
        assertThat(plans.get(0).orderedPickups()).containsExactlyInAnyOrder(a, b, c);
    }

    @Test
    @DisplayName("vehicle capacity is respected when enough vehicles are available")
    void capacityIsRespected() {
        PickupJob a = pickup("A", 40.80, -74.10, 40, 5);
        PickupJob b = pickup("B", 40.90, -74.00, 40, 5);
        PickupJob c = pickup("C", 41.10, -73.90, 40, 5);

        List<RouteOptimizer.RoutePlan> plans = optimizer.plan(depot(), List.of(a, b, c),
                List.of(vehicle("Small", 50), vehicle("Large", 100)));

        assertThat(plans).hasSize(2);
        assertThat(plans).allSatisfy(plan -> assertThat(plan.capacityRespected()).isTrue());
        assertThat(plans.stream().mapToInt(plan -> plan.orderedPickups().size()).sum()).isEqualTo(3);
    }

    @Test
    @DisplayName("a route that exceeds capacity is reported rather than silently dropped")
    void overCapacityIsFlagged() {
        PickupJob a = pickup("A", 40.80, -74.10, 60, 5);
        PickupJob b = pickup("B", 40.90, -74.00, 60, 5);

        List<RouteOptimizer.RoutePlan> plans = optimizer.plan(depot(), List.of(a, b),
                List.of(vehicle("Tiny", 10)));

        assertThat(plans).hasSize(1);
        assertThat(plans.get(0).capacityRespected()).isFalse();
        assertThat(plans.get(0).notes()).anyMatch(note -> note.contains("exceeds the capacity"));
    }

    @Test
    @DisplayName("the 2-opt pass never lengthens the tour")
    void twoOptDoesNotLengthenTheTour() {
        List<PickupJob> pickups = List.of(
                pickup("A", 40.80, -74.10, 1, 5),
                pickup("B", 41.20, -74.30, 1, 5),
                pickup("C", 40.90, -73.70, 1, 5),
                pickup("D", 41.10, -74.20, 1, 5),
                pickup("E", 40.70, -74.00, 1, 5));

        RouteOptimizer.RoutePlan plan = optimizer.plan(depot(), pickups, List.of(vehicle("Van", 50)))
                .get(0);

        double total = 0;
        double lat = depot().lat();
        double lon = depot().lon();
        for (PickupJob job : plan.orderedPickups()) {
            total += geo.distanceKm(lat, lon, job.getOriginLatitude().doubleValue(),
                    job.getOriginLongitude().doubleValue());
            lat = job.getOriginLatitude().doubleValue();
            lon = job.getOriginLongitude().doubleValue();
        }
        total += geo.distanceKm(lat, lon, depot().lat(), depot().lon());

        assertThat(total).isLessThanOrEqualTo(plan.totalDistanceKm() + 0.01);
    }

    @Test
    @DisplayName("higher priority pickups are placed first")
    void priorityIsRespected() {
        PickupJob low = pickup("Low", 40.80, -74.10, 1, 2);
        PickupJob high = pickup("High", 41.20, -74.30, 1, 9);

        RouteOptimizer.RoutePlan plan = optimizer.plan(depot(), List.of(low, high),
                List.of(vehicle("Van", 50))).get(0);

        assertThat(plan.orderedPickups().get(0)).isSameAs(high);
    }

    @Test
    @DisplayName("no pickups or no vehicles produces no routes")
    void emptyInput() {
        assertThat(optimizer.plan(depot(), List.of(), List.of(vehicle("Van", 50)))).isEmpty();
        assertThat(optimizer.plan(depot(), List.of(pickup("A", 40.8, -74.1, 1, 5)), List.of())).isEmpty();
    }
}
