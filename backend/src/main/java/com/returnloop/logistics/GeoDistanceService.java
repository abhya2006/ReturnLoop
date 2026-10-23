package com.returnloop.logistics;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Geospatial distance calculations.
 *
 * <p>Distances are great-circle estimates on the WGS-84 sphere. They are
 * <em>estimates</em>, not live navigation distances: no live traffic or road
 * network data is consulted. The API labels them as estimated throughout.
 */
@Service
public class GeoDistanceService {

    /** Mean earth radius in kilometres. */
    private static final double EARTH_RADIUS_KM = 6371.0088;

    /** Average road speed used to convert distance into a duration estimate. */
    private static final double AVERAGE_SPEED_KMH = 45.0;

    /**
     * Great-circle distance in kilometres between two coordinates.
     */
    public double distanceKm(double latitude1, double longitude1, double latitude2, double longitude2) {
        double dLat = Math.toRadians(latitude2 - latitude1);
        double dLon = Math.toRadians(longitude2 - longitude1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public double distanceKm(BigDecimal latitude1, BigDecimal longitude1,
                             BigDecimal latitude2, BigDecimal longitude2) {
        if (latitude1 == null || longitude1 == null || latitude2 == null || longitude2 == null) {
            return 0;
        }
        return distanceKm(latitude1.doubleValue(), longitude1.doubleValue(),
                latitude2.doubleValue(), longitude2.doubleValue());
    }

    public double distanceKm(double latitude1, double longitude1,
                             BigDecimal latitude2, BigDecimal longitude2) {
        if (latitude2 == null || longitude2 == null) {
            return 0;
        }
        return distanceKm(latitude1, longitude1, latitude2.doubleValue(), longitude2.doubleValue());
    }

    /**
     * Estimated driving duration in minutes for a great-circle distance.
     */
    public int estimatedDurationMinutes(double distanceKm) {
        return (int) Math.round(distanceKm / AVERAGE_SPEED_KMH * 60);
    }

    /**
     * Estimated transport cost for moving a given weight over a given distance,
     * using a simple per-kilometre, per-kilogram rate.
     */
    public BigDecimal transportCost(double distanceKm, double weightKg) {
        double ratePerKmPerKg = 0.045;
        return BigDecimal.valueOf(distanceKm * weightKg * ratePerKmPerKg).setScale(2, RoundingMode.HALF_UP);
    }
}
