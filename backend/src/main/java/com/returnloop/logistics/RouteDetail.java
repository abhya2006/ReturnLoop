package com.returnloop.logistics;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RouteDetail(
        UUID id,
        String routeNumber,
        RouteStatus status,
        String statusLabel,
        LocalDate routeDate,
        UUID vehicleId,
        String vehicleName,
        UUID driverId,
        String driverName,
        UUID originLocationId,
        String originName,
        int stopCount,
        int totalUnits,
        BigDecimal totalWeightKg,
        BigDecimal estimatedDistanceKm,
        Integer estimatedDurationMinutes,
        BigDecimal estimatedCost,
        String algorithm,
        String notes,
        boolean estimatedDistances,
        List<StopResponse> stops) {

    public static RouteDetail from(Route route, List<RouteStop> stops) {
        return new RouteDetail(
                route.getId(),
                route.getRouteNumber(),
                route.getStatus(),
                route.getStatus().getLabel(),
                route.getRouteDate(),
                route.getVehicleId(),
                route.getVehicleName(),
                route.getDriverId(),
                route.getDriverName(),
                route.getOriginLocationId(),
                route.getOriginName(),
                route.getStopCount(),
                route.getTotalUnits(),
                route.getTotalWeightKg(),
                route.getEstimatedDistanceKm(),
                route.getEstimatedDurationMinutes(),
                route.getEstimatedCost(),
                route.getAlgorithm(),
                route.getNotes(),
                true,
                stops.stream().map(StopResponse::from).toList());
    }

    public record StopResponse(UUID id, UUID pickupJobId, int sequence, String stopType, String locationName,
                               String address, BigDecimal latitude, BigDecimal longitude, int unitCount,
                               BigDecimal legDistanceKm, Integer legDurationMinutes, boolean completed,
                               Instant completedAt, String note) {
        static StopResponse from(RouteStop stop) {
            return new StopResponse(stop.getId(), stop.getPickupJobId(), stop.getSequence(), stop.getStopType(),
                    stop.getLocationName(), stop.getAddress(), stop.getLatitude(), stop.getLongitude(),
                    stop.getUnitCount(), stop.getLegDistanceKm(), stop.getLegDurationMinutes(), stop.isCompleted(),
                    stop.getCompletedAt(), stop.getNote());
        }
    }
}
