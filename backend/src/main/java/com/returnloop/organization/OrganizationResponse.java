package com.returnloop.organization;

import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        String slug,
        String contactEmail,
        String country,
        String defaultCurrency,
        boolean active,
        int holdingThresholdDays) {

    public static OrganizationResponse from(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                organization.getContactEmail(),
                organization.getCountry(),
                organization.getDefaultCurrency(),
                organization.isActive(),
                organization.getHoldingThresholdDays());
    }
}
