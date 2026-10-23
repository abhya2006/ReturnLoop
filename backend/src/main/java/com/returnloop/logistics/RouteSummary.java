package com.returnloop.logistics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RouteSummary(
        UUID id,
        String routeNumber,
        RouteStatus status,
        String statusLabel,
        LocalDate routeDate,
        UUID vehicleId,
        String vehicleName,
        UUID driverId,
        String driverName,
        String originName,
        int stopCount,
        int totalUnits,
        BigDecimal totalWeightKg,
        BigDecimal estimatedDistanceKm,
        Integer estimatedDurationMinutes,
        BigDecimal estimatedCost,
        String algorithm,
        String notes) {

    public static RouteSummary from(Route route) {
        return new RouteSummary(
                route.getId(),
                route.getRouteNumber(),
                route.getStatus(),
                route.getStatus().getLabel(),
                route.getRouteDate(),
                route.getVehicleId(),
                route.getVehicleName(),
                route.getDriverId(),
                route.getDriverName(),
                route.getOriginName(),
                route.getStopCount(),
                route.getTotalUnits(),
                route.getTotalWeightKg(),
                route.getEstimatedDistanceKm(),
                route.getEstimatedDurationMinutes(),
                route.getEstimatedCost(),
                route.getAlgorithm(),
                route.getNotes());
    }
}
