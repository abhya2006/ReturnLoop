package com.returnloop.logistics;

import com.returnloop.common.error.ApiException;
import com.returnloop.common.error.ErrorCode;
import com.returnloop.common.event.DomainEvent;
import com.returnloop.common.event.DomainEventPublisher;
import com.returnloop.common.web.PageResponse;
import com.returnloop.returns.ReturnRequest;
import com.returnloop.returns.ReturnService;
import com.returnloop.returns.ReturnStatus;
import com.returnloop.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pickup scheduling and route planning.
 */
@Service
@RequiredArgsConstructor
public class LogisticsService {

    private final PickupJobRepository pickupRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository stopRepository;
    private final LocationRepository locationRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final RouteOptimizer routeOptimizer;
    private final GeoDistanceService geo;
    private final ReturnService returnService;
    private final DomainEventPublisher eventPublisher;
    private final CurrentUser currentUser;

    // ---------------------------------------------------------------- pickups

    @Transactional(readOnly = true)
    public PageResponse<PickupJobResponse> listPickups(UUID organizationId, List<PickupStatus> statuses,
                                                        LocalDate date, UUID vehicleId, Pageable pageable) {
        Page<PickupJob> page = pickupRepository.search(organizationId,
                statuses == null || statuses.isEmpty() ? null : statuses, date, vehicleId, pageable);
        return PageResponse.from(page, PickupJobResponse::from);
    }

    @Transactional(readOnly = true)
    public PickupJobResponse getPickup(UUID organizationId, UUID pickupId) {
        return PickupJobResponse.from(requirePickup(organizationId, pickupId));
    }

    @Transactional
    public PickupJobResponse createPickup(UUID organizationId, CreatePickupRequest request) {
        if (request.scheduledDate() == null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "A pickup date is required");
        }
        if (request.windowStart() != null && request.windowEnd() != null
                && request.windowEnd().isBefore(request.windowStart())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "The pickup window must end after it starts");
        }

        PickupJob job = new PickupJob();
        job.setOrganizationId(organizationId);
        job.setPickupNumber(nextPickupNumber(organizationId));
        job.setReturnId(request.returnId());
        job.setOriginAddress(request.originAddress());
        job.setOriginLatitude(request.originLatitude());
        job.setOriginLongitude(request.originLongitude());
        job.setDestinationLocationId(request.destinationLocationId());
        job.setScheduledDate(request.scheduledDate());
        job.setWindowStart(request.windowStart());
        job.setWindowEnd(request.windowEnd());
        job.setPriority(request.priority() == null ? 5 : Math.min(Math.max(request.priority(), 1), 9));
        job.setUnitCount(request.unitCount() == null ? 1 : Math.max(request.unitCount(), 1));
        job.setTotalWeightKg(request.totalWeightKg());
        job.setNotes(request.notes());

        if (request.destinationLocationId() != null) {
            Location destination = locationRepository.findById(request.destinationLocationId())
                    .orElseThrow(() -> ApiException.notFound("Location", request.destinationLocationId()));
            job.setDestinationName(destination.getName());
        }

        if (request.scheduledDate() != null) {
            job.setStatus(PickupStatus.SCHEDULED);
        }
        PickupJob saved = pickupRepository.save(job);

        if (request.returnId() != null) {
            ReturnRequest returnRequest = returnService.require(organizationId, request.returnId());
            returnRequest.setPickupId(saved.getId());
            if (returnRequest.getStatus() == ReturnStatus.RETURN_CREATED) {
                returnService.transition(organizationId, request.returnId(), ReturnStatus.PICKUP_SCHEDULED,
                        actor(), "Pickup " + saved.getPickupNumber() + " scheduled for " + request.scheduledDate());
            }
        }

        eventPublisher.publish(DomainEvent.of("pickup.scheduled", organizationId, "PickupJob", saved.getId(),
                actor(), Map.of("pickupNumber", saved.getPickupNumber(),
                        "date", String.valueOf(saved.getScheduledDate()))));

        return PickupJobResponse.from(saved);
    }

    /**
     * Schedules an existing pickup request, assigning a vehicle and driver.
     */
    @Transactional
    public PickupJobResponse schedule(UUID organizationId, UUID pickupId, SchedulePickupRequest request) {
        PickupJob job = requirePickup(organizationId, pickupId);
        if (job.getStatus() != PickupStatus.REQUESTED && job.getStatus() != PickupStatus.SCHEDULED
                && job.getStatus() != PickupStatus.FAILED) {
            throw ApiException.invalidState("A " + job.getStatus() + " pickup cannot be rescheduled");
        }
        if (request.scheduledDate() == null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "A pickup date is required");
        }
        if (request.vehicleId() != null) {
            Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                    .orElseThrow(() -> ApiException.notFound("Vehicle", request.vehicleId()));
            if (!vehicle.getOrganizationId().equals(organizationId)) {
                throw new ApiException(ErrorCode.CROSS_TENANT_ACCESS);
            }
            int alreadyLoaded = pickupRepository
                    .findByOrganizationIdAndStatusIn(organizationId,
                            List.of(PickupStatus.SCHEDULED, PickupStatus.IN_TRANSIT))
                    .stream()
                    .filter(p -> request.vehicleId().equals(p.getVehicleId()))
                    .mapToInt(PickupJob::getUnitCount)
                    .sum();
            if (alreadyLoaded + job.getUnitCount() > vehicle.getCapacityUnits()) {
                throw new ApiException(ErrorCode.CAPACITY_EXCEEDED,
                        "Assigning this pickup would exceed the capacity of " + vehicle.getName()
                                + " (" + vehicle.getCapacityUnits() + " units)");
            }
            job.setVehicleId(vehicle.getId());
        }
        if (request.driverId() != null) {
            Driver driver = driverRepository.findById(request.driverId())
                    .orElseThrow(() -> ApiException.notFound("Driver", request.driverId()));
            if (!driver.getOrganizationId().equals(organizationId)) {
                throw new ApiException(ErrorCode.CROSS_TENANT_ACCESS);
            }
            job.setDriverId(driver.getId());
        }
        job.setScheduledDate(request.scheduledDate());
        job.setWindowStart(request.windowStart());
        job.setWindowEnd(request.windowEnd());
        job.setPriority(request.priority() == null ? job.getPriority() : request.priority());
        job.setStatus(PickupStatus.SCHEDULED);
        PickupJob saved = pickupRepository.save(job);

        if (job.getReturnId() != null && job.getReturnId() != null) {
            ReturnRequest returnRequest = returnService.require(organizationId, job.getReturnId());
            if (returnRequest.getStatus() == ReturnStatus.RETURN_CREATED) {
                returnService.transition(organizationId, job.getReturnId(), ReturnStatus.PICKUP_SCHEDULED,
                        actor(), "Pickup scheduled");
            }
        }
        eventPublisher.publish(DomainEvent.of("pickup.scheduled", organizationId, "PickupJob", saved.getId(),
                actor(), Map.of("pickupNumber", saved.getPickupNumber())));
        return PickupJobResponse.from(saved);
    }

    @Transactional
    public PickupJobResponse updatePickupStatus(UUID organizationId, UUID pickupId, PickupStatus target,
                                                 String note) {
        PickupJob job = requirePickup(organizationId, pickupId);
        PickupStatus current = job.getStatus();
        if (current == target) {
            return PickupJobResponse.from(job);
        }
        if (!current.canTransitionTo(target)) {
            throw ApiException.invalidState("Cannot move a pickup from " + current + " to " + target
                    + ". Allowed next steps: " + current.allowedTransitions());
        }
        if (target == PickupStatus.FAILED && (note == null || note.isBlank())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "A failure reason is required");
        }
        job.setStatus(target);
        if (target == PickupStatus.COLLECTED || target == PickupStatus.DELIVERED) {
            job.setCompletedAt(Instant.now());
        }
        if (target == PickupStatus.FAILED) {
            job.setFailureReason(note);
        }
        if (note != null && !note.isBlank()) {
            job.setEvidenceNote(note);
        }
        PickupJob saved = pickupRepository.save(job);

        if (job.getReturnId() != null) {
            ReturnRequest returnRequest = returnService.require(organizationId, job.getReturnId());
            if (target == PickupStatus.COLLECTED && returnRequest.getStatus() == ReturnStatus.PICKUP_SCHEDULED) {
                returnService.transition(organizationId, job.getReturnId(), ReturnStatus.COLLECTED, actor(),
                        "Goods collected");
            } else if (target == PickupStatus.DELIVERED
                    && returnRequest.getStatus() == ReturnStatus.COLLECTED) {
                returnService.transition(organizationId, job.getReturnId(), ReturnStatus.WAREHOUSE_RECEIVED,
                        actor(), "Goods received at " + (job.getDestinationName() == null ? "the warehouse"
                                : job.getDestinationName()));
            }
        }

        eventPublisher.publish(DomainEvent.of("pickup.status-changed", organizationId, "PickupJob", saved.getId(),
                actor(), Map.of("from", current.name(), "to", target.name())));
        return PickupJobResponse.from(saved);
    }

    /**
     * Manually reassigns a pickup to a different vehicle or driver.
     */
    @Transactional
    public PickupJobResponse reassign(UUID organizationId, UUID pickupId, UUID vehicleId, UUID driverId) {
        PickupJob job = requirePickup(organizationId, pickupId);
        if (vehicleId != null) {
            Vehicle vehicle = vehicleRepository.findById(vehicleId)
                    .orElseThrow(() -> ApiException.notFound("Vehicle", vehicleId));
            if (!vehicle.getOrganizationId().equals(organizationId)) {
                throw new ApiException(ErrorCode.CROSS_TENANT_ACCESS);
            }
            job.setVehicleId(vehicleId);
        }
        if (driverId != null) {
            Driver driver = driverRepository.findById(driverId)
                    .orElseThrow(() -> ApiException.notFound("Driver", driverId));
            if (!driver.getOrganizationId().equals(organizationId)) {
                throw new ApiException(ErrorCode.CROSS_TENANT_ACCESS);
            }
            job.setDriverId(driverId);
        }
        return PickupJobResponse.from(pickupRepository.save(job));
    }

    // ----------------------------------------------------------------- routes

    /**
     * Builds routes for a date from the scheduled pickups, then persists them.
     */
    @Transactional
    public List<RouteDetail> optimize(UUID organizationId, LocalDate date, OptimizeRequest request) {
        List<PickupJob> pickups = pickupRepository.findByOrganizationIdAndScheduledDate(organizationId, date)
                .stream()
                .filter(job -> job.getStatus() == PickupStatus.SCHEDULED
                        || job.getStatus() == PickupStatus.REQUESTED)
                .filter(job -> request.includeAssigned() || job.getVehicleId() == null)
                .toList();
        if (pickups.isEmpty()) {
            return List.of();
        }

        List<Vehicle> vehicles = vehicleRepository.findByOrganizationIdAndActiveTrue(organizationId);
        if (vehicles.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_STATE_TRANSITION,
                    "No active vehicles are available to plan routes with");
        }

        Depot depot = resolveDepot(organizationId, request.depotLocationId());
        List<RouteOptimizer.RoutePlan> plans = routeOptimizer.plan(depot, pickups, vehicles);

        List<RouteDetail> results = new ArrayList<>();
        for (RouteOptimizer.RoutePlan plan : plans) {
            Route route = new Route();
            route.setOrganizationId(organizationId);
            route.setRouteNumber(nextRouteNumber(organizationId));
            route.setStatus(RouteStatus.PLANNED);
            route.setRouteDate(date);
            route.setVehicleId(plan.vehicle().getId());
            route.setVehicleName(plan.vehicle().getName());
            route.setStopCount(plan.orderedPickups().size());
            route.setTotalUnits(plan.orderedPickups().stream().mapToInt(PickupJob::getUnitCount).sum());
            route.setTotalWeightKg(BigDecimal.valueOf(plan.totalWeightKg()).setScale(3, RoundingMode.HALF_UP));
            route.setEstimatedDistanceKm(BigDecimal.valueOf(plan.totalDistanceKm()).setScale(3, RoundingMode.HALF_UP));
            route.setEstimatedDurationMinutes(plan.totalDurationMinutes());
            route.setEstimatedCost(geo.transportCost(plan.totalDistanceKm(), plan.totalWeightKg()));
            route.setAlgorithm(routeOptimizer.algorithmName());
            route.setOriginLocationId(depot.id());
            route.setOriginName(depot.name());
            route.setNotes(String.join("; ", plan.notes()));
            Route saved = routeRepository.save(route);

            List<RouteStop> stops = routeOptimizer.toStops(depot, plan, saved.getId());
            stopRepository.saveAll(stops);

            for (int i = 0; i < plan.orderedPickups().size(); i++) {
                PickupJob job = plan.orderedPickups().get(i);
                job.setRouteId(saved.getId());
                job.setStopSequence(i + 1);
                job.setVehicleId(plan.vehicle().getId());
                job.setEstimatedDistanceKm(stops.get(i).getLegDistanceKm());
                job.setEstimatedDurationMinutes(stops.get(i).getLegDurationMinutes());
                job.setEstimatedCost(geo.transportCost(
                        stops.get(i).getLegDistanceKm().doubleValue(),
                        job.getTotalWeightKg() == null ? 0 : job.getTotalWeightKg().doubleValue()));
                pickupRepository.save(job);
            }
            results.add(getRoute(organizationId, saved.getId()));
        }

        eventPublisher.publish(DomainEvent.of("route.optimized", organizationId, "Route", null,
                actor(), Map.of("date", String.valueOf(date), "routes", results.size())));
        return results;
    }

    @Transactional(readOnly = true)
    public PageResponse<RouteSummary> listRoutes(UUID organizationId, RouteStatus status, LocalDate date,
                                                  Pageable pageable) {
        Page<Route> page = routeRepository.search(organizationId, status, date, pageable);
        return PageResponse.from(page, RouteSummary::from);
    }

    @Transactional(readOnly = true)
    public RouteDetail getRoute(UUID organizationId, UUID routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> ApiException.notFound("Route", routeId));
        if (!route.getOrganizationId().equals(organizationId)) {
            throw new ApiException(ErrorCode.CROSS_TENANT_ACCESS);
        }
        List<RouteStop> stops = stopRepository.findByRouteIdOrderBySequenceAsc(routeId);
        return RouteDetail.from(route, stops);
    }

    @Transactional
    public RouteDetail updateRouteStatus(UUID organizationId, UUID routeId, RouteStatus status) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> ApiException.notFound("Route", routeId));
        if (!route.getOrganizationId().equals(organizationId)) {
            throw new ApiException(ErrorCode.CROSS_TENANT_ACCESS);
        }
        route.setStatus(status);
        routeRepository.save(route);
        return getRoute(organizationId, routeId);
    }

    /**
     * Marks a route stop complete, which also advances its pickup.
     */
    @Transactional
    public RouteDetail completeStop(UUID organizationId, UUID routeId, UUID stopId, String evidence) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> ApiException.notFound("Route", routeId));
        if (!route.getOrganizationId().equals(organizationId)) {
            throw new ApiException(ErrorCode.CROSS_TENANT_ACCESS);
        }
        RouteStop stop = stopRepository.findById(stopId)
                .orElseThrow(() -> ApiException.notFound("Route stop", stopId));
        if (!stop.getRouteId().equals(routeId)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "This stop does not belong to the route");
        }
        if (stop.isCompleted()) {
            throw ApiException.conflict("This stop is already complete");
        }
        stop.setCompleted(true);
        stop.setCompletedAt(Instant.now());
        stop.setNote(evidence);
        stopRepository.save(stop);

        if (stop.getPickupJobId() != null) {
            PickupJob job = pickupRepository.findById(stop.getPickupJobId()).orElse(null);
            if (job != null) {
                job.setStatus(PickupStatus.COLLECTED);
                job.setEvidenceNote(evidence);
                job.setCompletedAt(Instant.now());
                pickupRepository.save(job);
                if (job.getReturnId() != null) {
                    ReturnRequest returnRequest = returnService.require(organizationId, job.getReturnId());
                    if (returnRequest.getStatus() == ReturnStatus.PICKUP_SCHEDULED) {
                        returnService.transition(organizationId, job.getReturnId(), ReturnStatus.COLLECTED,
                                actor(), "Goods collected");
                    }
                }
            }
        }
        return getRoute(organizationId, routeId);
    }

    // -------------------------------------------------------------- resources

    @Transactional(readOnly = true)
    public List<LocationResponse> listLocations(UUID organizationId) {
        return locationRepository.findByOrganizationIdAndActiveTrue(organizationId).stream()
                .map(LocationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> listVehicles(UUID organizationId) {
        return vehicleRepository.findByOrganizationIdOrderByNameAsc(organizationId).stream()
                .map(VehicleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DriverResponse> listDrivers(UUID organizationId) {
        return driverRepository.findByOrganizationIdAndActiveTrue(organizationId).stream()
                .map(DriverResponse::from)
                .toList();
    }

    // ---------------------------------------------------------------- helpers

    private Depot resolveDepot(UUID organizationId, UUID depotLocationId) {
        if (depotLocationId != null) {
            Location location = locationRepository.findById(depotLocationId)
                    .orElseThrow(() -> ApiException.notFound("Location", depotLocationId));
            if (!location.getOrganizationId().equals(organizationId)) {
                throw new ApiException(ErrorCode.CROSS_TENANT_ACCESS);
            }
            return new Depot(location.getId(), location.getName(), location.getLatitude(),
                    location.getLongitude());
        }
        Location defaultWarehouse = locationRepository
                .findFirstByOrganizationIdAndLocationTypeOrderByNameAsc(organizationId, LocationType.WAREHOUSE)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_STATE_TRANSITION,
                        "No warehouse location is configured; provide a depot location for route planning"));
        return new Depot(defaultWarehouse.getId(), defaultWarehouse.getName(), defaultWarehouse.getLatitude(),
                defaultWarehouse.getLongitude());
    }

    private PickupJob requirePickup(UUID organizationId, UUID pickupId) {
        PickupJob job = pickupRepository.findById(pickupId)
                .orElseThrow(() -> ApiException.notFound("Pickup", pickupId));
        if (!job.getOrganizationId().equals(organizationId)) {
            throw new ApiException(ErrorCode.CROSS_TENANT_ACCESS);
        }
        return job;
    }

    private String nextPickupNumber(UUID organizationId) {
        return "PU-" + String.format("%05d", pickupRepository.countByOrganizationId(organizationId) + 1);
    }

    private String nextRouteNumber(UUID organizationId) {
        return "RT-" + String.format("%05d", routeRepository.countByOrganizationId(organizationId) + 1);
    }

    private String actor() {
        return currentUser.get().map(u -> u.fullName()).orElse("system");
    }

    public record CreatePickupRequest(UUID returnId, String originAddress, BigDecimal originLatitude,
                                      BigDecimal originLongitude, UUID destinationLocationId,
                                      LocalDate scheduledDate, java.time.LocalTime windowStart,
                                      java.time.LocalTime windowEnd, Integer priority, Integer unitCount,
                                      BigDecimal totalWeightKg, String notes) {
    }

    public record SchedulePickupRequest(LocalDate scheduledDate, java.time.LocalTime windowStart,
                                        java.time.LocalTime windowEnd, UUID vehicleId, UUID driverId,
                                        Integer priority) {
    }

    public record OptimizeRequest(UUID depotLocationId, Boolean includeAssigned) {
    }
}
