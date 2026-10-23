package com.returnloop.logistics;

import java.math.BigDecimal;
import java.util.UUID;

public record VehicleResponse(
        UUID id,
        String name,
        String registration,
        String vehicleType,
        int capacityUnits,
        BigDecimal capacityKg,
        UUID homeLocationId,
        boolean active,
        String notes) {

    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getName(),
                vehicle.getRegistration(),
                vehicle.getVehicleType(),
                vehicle.getCapacityUnits(),
                vehicle.getCapacityKg(),
                vehicle.getHomeLocationId(),
                vehicle.isActive(),
                vehicle.getNotes());
    }
}
