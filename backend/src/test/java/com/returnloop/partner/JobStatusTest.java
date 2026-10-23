package com.returnloop.partner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobStatusTest {

    @Test
    @DisplayName("a job walks the full partner lifecycle")
    void fullLifecycle() {
        assertThat(JobStatus.PROPOSED.canTransitionTo(JobStatus.ASSIGNED)).isTrue();
        assertThat(JobStatus.ASSIGNED.canTransitionTo(JobStatus.ACCEPTED)).isTrue();
        assertThat(JobStatus.ACCEPTED.canTransitionTo(JobStatus.PROCESSING)).isTrue();
        assertThat(JobStatus.PROCESSING.canTransitionTo(JobStatus.AWAITING_REVIEW)).isTrue();
        assertThat(JobStatus.AWAITING_REVIEW.canTransitionTo(JobStatus.COMPLETED)).isTrue();
    }

    @Test
    @DisplayName("a partner may reject or fail a job")
    void rejectionAndFailure() {
        assertThat(JobStatus.ASSIGNED.canTransitionTo(JobStatus.REJECTED)).isTrue();
        assertThat(JobStatus.ACCEPTED.canTransitionTo(JobStatus.FAILED)).isTrue();
        assertThat(JobStatus.PROCESSING.canTransitionTo(JobStatus.FAILED)).isTrue();
    }

    @Test
    @DisplayName("a job cannot skip the acceptance step")
    void cannotSkipAcceptance() {
        assertThat(JobStatus.ASSIGNED.canTransitionTo(JobStatus.PROCESSING)).isFalse();
        assertThat(JobStatus.PROPOSED.canTransitionTo(JobStatus.COMPLETED)).isFalse();
        assertThat(JobStatus.ASSIGNED.canTransitionTo(JobStatus.COMPLETED)).isFalse();
    }

    @Test
    @DisplayName("a completed job is terminal")
    void completedIsTerminal() {
        assertThat(JobStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(JobStatus.COMPLETED.allowedTransitions()).isEmpty();
    }
}
