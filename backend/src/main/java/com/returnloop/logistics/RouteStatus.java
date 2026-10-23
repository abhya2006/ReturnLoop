package com.returnloop.logistics;

/**
 * The lifecycle of a planned route.
 */
public enum RouteStatus {

    PLANNED("Planned"),
    ASSIGNED("Assigned"),
    IN_PROGRESS("In progress"),
    COMPLETED("Completed"),
    DELAYED("Delayed"),
    CANCELLED("Cancelled");

    private final String label;

    RouteStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
