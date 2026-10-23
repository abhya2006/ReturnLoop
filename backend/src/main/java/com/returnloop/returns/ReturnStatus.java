package com.returnloop.returns;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * The reverse-logistics lifecycle.
 *
 * <p>Each status declares the statuses it may legally transition to. The
 * transition table is the single source of truth for the state machine and is
 * exercised directly by unit tests.
 */
public enum ReturnStatus {

    /** A return request exists but nothing has been arranged yet. */
    RETURN_CREATED("Return created"),

    /** A collection window and carrier/vehicle have been committed. */
    PICKUP_SCHEDULED("Pickup scheduled"),

    /** The goods have been collected from the customer. */
    COLLECTED("Collected"),

    /** The goods have been received and booked into a warehouse. */
    WAREHOUSE_RECEIVED("Warehouse received"),

    /** A warehouse inspection has been completed. */
    INSPECTED("Inspected"),

    /** Recovery options have been calculated and a decision is pending. */
    RECOVERY_DECISION("Recovery decision"),

    /** A partner has been assigned to carry out the recovery work. */
    PARTNER_ASSIGNED("Partner assigned"),

    /** The partner is actively processing the goods. */
    PROCESSING("Processing"),

    /** A recovery path has been executed and value realised. */
    RECOVERED("Recovered"),

    /** The return has been closed out. */
    COMPLETED("Completed"),

    /** The return was cancelled before recovery. */
    CANCELLED("Cancelled");

    private final String label;

    ReturnStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    private static final Map<ReturnStatus, Set<ReturnStatus>> TRANSITIONS = Map.ofEntries(
            Map.entry(RETURN_CREATED, EnumSet.of(PICKUP_SCHEDULED, CANCELLED)),
            Map.entry(PICKUP_SCHEDULED, EnumSet.of(COLLECTED, PICKUP_SCHEDULED, CANCELLED)),
            Map.entry(COLLECTED, EnumSet.of(WAREHOUSE_RECEIVED, CANCELLED)),
            Map.entry(WAREHOUSE_RECEIVED, EnumSet.of(INSPECTED, CANCELLED)),
            Map.entry(INSPECTED, EnumSet.of(RECOVERY_DECISION, CANCELLED)),
            Map.entry(RECOVERY_DECISION, EnumSet.of(PARTNER_ASSIGNED, RECOVERY_DECISION, CANCELLED)),
            Map.entry(PARTNER_ASSIGNED, EnumSet.of(PROCESSING, CANCELLED)),
            Map.entry(PROCESSING, EnumSet.of(RECOVERED, CANCELLED)),
            Map.entry(RECOVERED, EnumSet.of(COMPLETED)),
            Map.entry(COMPLETED, EnumSet.noneOf(ReturnStatus.class)),
            Map.entry(CANCELLED, EnumSet.noneOf(ReturnStatus.class)));

    public boolean canTransitionTo(ReturnStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public Set<ReturnStatus> allowedTransitions() {
        return Set.copyOf(TRANSITIONS.getOrDefault(this, Set.of()));
    }

    /** True when the return can no longer be modified or cancelled. */
    public boolean isTerminal() {
        return allowedTransitions().isEmpty();
    }
}
