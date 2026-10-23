package com.returnloop.partner;

/**
 * The kinds of recovery partner.
 */
public enum PartnerType {

    REPAIR("Repair provider"),
    REFURBISHMENT("Refurbishment centre"),
    RESALE("Resale partner"),
    LIQUIDATION("Liquidation business"),
    RECYCLING("Recycling provider"),
    LOGISTICS("Logistics provider");

    private final String label;

    PartnerType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
