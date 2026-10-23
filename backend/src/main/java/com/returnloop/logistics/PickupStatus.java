package com.returnloop.logistics;

/**
 * The lifecycle of a pickup job.
 */
public enum PickupStatus {

    REQUESTED("Requested"),
    SCHEDULED("Scheduled"),
    IN_TRANSIT("In transit"),
    COLLECTED("Collected"),
    DELIVERED("Delivered"),
    FAILED("Failed"),
    CANCELLED("Cancelled");

    private final String label;

    PickupStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    private static final java.util.Map<PickupStatus, java.util.Set<PickupStatus>> TRANSITIONS = java.util.Map.ofEntries(
            java.util.Map.entry(REQUESTED, java.util.EnumSet.of(SCHEDULED, CANCELLED)),
            java.util.Map.entry(SCHEDULED, java.util.EnumSet.of(IN_TRANSIT, FAILED, CANCELLED, SCHEDULED)),
            java.util.Map.entry(IN_TRANSIT, java.util.EnumSet.of(COLLECTED, FAILED, CANCELLED)),
            java.util.Map.entry(COLLECTED, java.util.EnumSet.of(DELIVERED, CANCELLED)),
            java.util.Map.entry(DELIVERED, java.util.EnumSet.noneOf(PickupStatus.class)),
            java.util.Map.entry(FAILED, java.util.EnumSet.of(SCHEDULED, CANCELLED)),
            java.util.Map.entry(CANCELLED, java.util.EnumSet.noneOf(PickupStatus.class)));

    public boolean canTransitionTo(PickupStatus target) {
        return TRANSITIONS.getOrDefault(this, java.util.Set.of()).contains(target);
    }

    public java.util.Set<PickupStatus> allowedTransitions() {
        return java.util.Set.copyOf(TRANSITIONS.getOrDefault(this, java.util.Set.of()));
    }
}
