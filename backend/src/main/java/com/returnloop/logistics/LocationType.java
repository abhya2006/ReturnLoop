package com.returnloop.logistics;

/**
 * The kinds of physical location the platform tracks.
 */
public enum LocationType {

    WAREHOUSE("Warehouse"),
    PICKUP_POINT("Pickup point"),
    PARTNER_FACILITY("Partner facility"),
    CUSTOMER("Customer");

    private final String label;

    LocationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
