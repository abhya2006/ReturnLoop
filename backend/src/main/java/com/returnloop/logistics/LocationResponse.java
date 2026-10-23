package com.returnloop.logistics;

import java.math.BigDecimal;
import java.util.UUID;

public record LocationResponse(
        UUID id,
        String name,
        LocationType locationType,
        String locationTypeLabel,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String region,
        Integer capacityUnits,
        boolean active) {

    public static LocationResponse from(Location location) {
        return new LocationResponse(
                location.getId(),
                location.getName(),
                location.getLocationType(),
                location.getLocationType().getLabel(),
                location.getAddress(),
                location.getLatitude(),
                location.getLongitude(),
                location.getRegion(),
                location.getCapacityUnits(),
                location.isActive());
    }
}
