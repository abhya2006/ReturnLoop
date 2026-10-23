package com.returnloop.recovery;

/**
 * The recovery destinations a returned product can be routed to.
 */
public enum RecoveryPath {

    /** Return to sellable stock without any work. */
    RESTOCK_AS_NEW("Restock as new"),

    /** Sell as open-box with a discount, no refurbishment work. */
    OPEN_BOX_RESALE("Open-box resale"),

    /** Repair to a working state, then resell. */
    REPAIR("Repair"),

    /** Full refurbishment, then resell. */
    REFURBISHMENT("Refurbishment"),

    /** Wholesale liquidation of working goods. */
    LIQUIDATION("Liquidation"),

    /** Harvest usable components for parts. */
    SPARE_PARTS("Spare parts recovery"),

    /** Material recycling. */
    RECYCLING("Recycling"),

    /** Disposal where no recovery route is viable. */
    SAFE_DISPOSAL("Safe disposal");

    private final String label;

    RecoveryPath(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
