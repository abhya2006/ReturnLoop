package com.returnloop.logistics;

import com.returnloop.common.web.ApiResponse;
import com.returnloop.common.web.PageResponse;
import com.returnloop.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/logistics")
@RequiredArgsConstructor
@Tag(name = "Logistics", description = "Pickup scheduling and route optimization")
public class LogisticsController {

    private final LogisticsService service;
    private final CurrentUser currentUser;

    // ------------------------------------------------------------- pickups

    @GetMapping("/pickups")
    public PageResponse<PickupJobResponse> listPickups(
            @RequestParam(required = false) List<PickupStatus> status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID vehicleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Order.asc("scheduledDate"), Sort.Order.asc("priority")));
        return service.listPickups(currentUser.organizationId(), status, date, vehicleId, pageable);
    }

    @GetMapping("/pickups/{id}")
    public ApiResponse<PickupJobResponse> getPickup(@PathVariable UUID id) {
        return ApiResponse.of(service.getPickup(currentUser.organizationId(), id));
    }

    @PostMapping("/pickups")
    @Operation(summary = "Create and schedule a pickup")
    public ApiResponse<PickupJobResponse> createPickup(@RequestBody LogisticsService.CreatePickupRequest request) {
        return ApiResponse.of(service.createPickup(currentUser.organizationId(), request));
    }

    @PostMapping("/pickups/{id}/schedule")
    public ApiResponse<PickupJobResponse> schedule(@PathVariable UUID id,
                                                   @RequestBody LogisticsService.SchedulePickupRequest request) {
        return ApiResponse.of(service.schedule(currentUser.organizationId(), id, request));
    }

    @PostMapping("/pickups/{id}/status")
    public ApiResponse<PickupJobResponse> updateStatus(@PathVariable UUID id,
                                                       @RequestBody StatusRequest request) {
        return ApiResponse.of(service.updatePickupStatus(currentUser.organizationId(), id, request.status(),
                request.note()));
    }

    @PostMapping("/pickups/{id}/reassign")
    public ApiResponse<PickupJobResponse> reassign(@PathVariable UUID id,
                                                   @RequestBody ReassignRequest request) {
        return ApiResponse.of(service.reassign(currentUser.organizationId(), id, request.vehicleId(),
                request.driverId()));
    }

    // -------------------------------------------------------------- routes

    @PostMapping("/routes/optimize")
    @Operation(summary = "Build capacity-constrained routes for a date")
    public ApiResponse<List<RouteDetail>> optimize(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody(required = false) LogisticsService.OptimizeRequest request) {
        LogisticsService.OptimizeRequest effective = request == null
                ? new LogisticsService.OptimizeRequest(null, false) : request;
        return ApiResponse.of(service.optimize(currentUser.organizationId(), date, effective));
    }

    @GetMapping("/routes")
    public PageResponse<RouteSummary> listRoutes(
            @RequestParam(required = false) RouteStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Order.desc("routeDate"), Sort.Order.desc("createdAt")));
        return service.listRoutes(currentUser.organizationId(), status, date, pageable);
    }

    @GetMapping("/routes/{id}")
    public ApiResponse<RouteDetail> getRoute(@PathVariable UUID id) {
        return ApiResponse.of(service.getRoute(currentUser.organizationId(), id));
    }

    @PostMapping("/routes/{id}/status")
    public ApiResponse<RouteDetail> updateRouteStatus(@PathVariable UUID id,
                                                      @RequestBody RouteStatusRequest request) {
        return ApiResponse.of(service.updateRouteStatus(currentUser.organizationId(), id, request.status()));
    }

    @PostMapping("/routes/{routeId}/stops/{stopId}/complete")
    @Operation(summary = "Complete a stop and record the collection evidence")
    public ApiResponse<RouteDetail> completeStop(@PathVariable UUID routeId, @PathVariable UUID stopId,
                                                 @RequestBody EvidenceRequest request) {
        return ApiResponse.of(service.completeStop(currentUser.organizationId(), routeId, stopId, request.note()));
    }

    // ----------------------------------------------------------- resources

    @GetMapping("/locations")
    public ApiResponse<List<LocationResponse>> locations() {
        return ApiResponse.of(service.listLocations(currentUser.organizationId()));
    }

    @GetMapping("/vehicles")
    public ApiResponse<List<VehicleResponse>> vehicles() {
        return ApiResponse.of(service.listVehicles(currentUser.organizationId()));
    }

    @GetMapping("/drivers")
    public ApiResponse<List<DriverResponse>> drivers() {
        return ApiResponse.of(service.listDrivers(currentUser.organizationId()));
    }

    public record StatusRequest(PickupStatus status, String note) {
    }

    public record ReassignRequest(UUID vehicleId, UUID driverId) {
    }

    public record RouteStatusRequest(RouteStatus status) {
    }

    public record EvidenceRequest(String note) {
    }
}
