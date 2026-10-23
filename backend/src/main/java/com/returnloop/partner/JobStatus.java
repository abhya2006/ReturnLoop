package com.returnloop.partner;

/**
 * The lifecycle of a recovery job assigned to a partner.
 */
public enum JobStatus {

    PROPOSED("Proposed"),
    ASSIGNED("Assigned"),
    ACCEPTED("Accepted"),
    PROCESSING("Processing"),
    COMPLETED("Completed"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled"),
    FAILED("Failed"),
    AWAITING_REVIEW("Awaiting review");

    private final String label;

    JobStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    private static final java.util.Map<JobStatus, java.util.Set<JobStatus>> TRANSITIONS = java.util.Map.ofEntries(
            java.util.Map.entry(PROPOSED, java.util.EnumSet.of(ASSIGNED, CANCELLED)),
            java.util.Map.entry(ASSIGNED, java.util.EnumSet.of(ACCEPTED, REJECTED, CANCELLED)),
            java.util.Map.entry(ACCEPTED, java.util.EnumSet.of(PROCESSING, FAILED, CANCELLED)),
            java.util.Map.entry(PROCESSING, java.util.EnumSet.of(AWAITING_REVIEW, COMPLETED, FAILED, CANCELLED)),
            java.util.Map.entry(AWAITING_REVIEW, java.util.EnumSet.of(COMPLETED, FAILED, CANCELLED)),
            java.util.Map.entry(COMPLETED, java.util.EnumSet.noneOf(JobStatus.class)),
            java.util.Map.entry(REJECTED, java.util.EnumSet.noneOf(JobStatus.class)),
            java.util.Map.entry(CANCELLED, java.util.EnumSet.noneOf(JobStatus.class)),
            java.util.Map.entry(FAILED, java.util.EnumSet.noneOf(JobStatus.class)));

    public boolean canTransitionTo(JobStatus target) {
        return TRANSITIONS.getOrDefault(this, java.util.Set.of()).contains(target);
    }

    public java.util.Set<JobStatus> allowedTransitions() {
        return java.util.Set.copyOf(TRANSITIONS.getOrDefault(this, java.util.Set.of()));
    }

    public boolean isTerminal() {
        return allowedTransitions().isEmpty();
    }
}
