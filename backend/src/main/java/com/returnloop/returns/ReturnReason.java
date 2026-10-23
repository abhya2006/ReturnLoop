package com.returnloop.returns;

/**
 * Why a product came back. The reason feeds the recovery engine: a faulty
 * product is routed to repair, while an over-purchase is a resale candidate.
 */
public enum ReturnReason {

    DEFECTIVE("Defective or faulty"),
    DAMAGED_IN_TRANSIT("Damaged in transit"),
    WRONG_ITEM("Wrong item delivered"),
    NOT_AS_DESCRIBED("Not as described"),
    CHANGED_MIND("Customer changed mind"),
    OVERPURCHASE("Over-purchase / unneeded"),
    DUPLICATE_ORDER("Duplicate order"),
    NO_LONGER_NEEDED("No longer needed"),
    RECALL("Recall or safety recall"),
    SURPLUS_UNSOLD("Unsold surplus / inventory"),
    END_OF_LIFE("End of life / obsolete"),
    OTHER("Other");

    private final String label;

    ReturnReason(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
