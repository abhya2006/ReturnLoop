package com.returnloop.logistics;

import java.util.UUID;

public record DriverResponse(
        UUID id,
        String name,
        String phone,
        String email,
        String licenseNumber,
        boolean active) {

    public static DriverResponse from(Driver driver) {
        return new DriverResponse(driver.getId(), driver.getName(), driver.getPhone(), driver.getEmail(),
                driver.getLicenseNumber(), driver.isActive());
    }
}
