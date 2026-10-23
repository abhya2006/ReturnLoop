package com.returnloop.inventory;

/**
 * The state of goods held in a warehouse.
 */
public enum InventoryStatus {

    /** Booked in, not yet allocated to a recovery path. */
    AVAILABLE("Available"),
    /** Allocated to a recovery job and awaiting dispatch. */
    RESERVED("Reserved"),
    /** Physically damaged, awaiting a decision. */
    DAMAGED("Damaged"),
    /** Work is being carried out on the goods. */
    PROCESSING("Processing"),
    /** Goods have left the warehouse. */
    DISPATCHED("Dispatched"),
    /** Goods have been received back after a recovery job. */
    COMPLETED("Completed"),
    /** Goods are quarantined pending a safety decision. */
    QUARANTINED("Quarantined");

    private final String label;

    InventoryStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
