package com.returnloop.logistics;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The depot a route starts and ends at.
 *
 * @param id        the location id, or null for a synthetic depot
 * @param name      display name
 * @param latitude  latitude in decimal degrees
 * @param longitude longitude in decimal degrees
 */
public record Depot(UUID id, String name, BigDecimal latitude, BigDecimal longitude) {

    public double lat() {
        return latitude == null ? 0 : latitude.doubleValue();
    }

    public double lon() {
        return longitude == null ? 0 : longitude.doubleValue();
    }
}
