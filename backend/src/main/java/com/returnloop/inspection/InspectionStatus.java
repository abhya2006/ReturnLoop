package com.returnloop.inspection;

/**
 * Inspection lifecycle states.
 */
public enum InspectionStatus {

    AWAITING_INSPECTION("Awaiting inspection"),
    IN_PROGRESS("In progress"),
    PASSED("Passed"),
    DAMAGED("Damaged"),
    INCOMPLETE("Incomplete"),
    UNSAFE("Unsafe");

    private final String label;

    InspectionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isFinal() {
        return this != AWAITING_INSPECTION && this != IN_PROGRESS;
    }
}
