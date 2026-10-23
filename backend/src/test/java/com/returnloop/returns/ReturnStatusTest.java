package com.returnloop.returns;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The lifecycle state machine is the contract every screen depends on, so the
 * transition table is tested directly.
 */
class ReturnStatusTest {

    @Test
    @DisplayName("a return walks the full happy path")
    void fullLifecycle() {
        assertThat(ReturnStatus.RETURN_CREATED.canTransitionTo(ReturnStatus.PICKUP_SCHEDULED)).isTrue();
        assertThat(ReturnStatus.PICKUP_SCHEDULED.canTransitionTo(ReturnStatus.COLLECTED)).isTrue();
        assertThat(ReturnStatus.COLLECTED.canTransitionTo(ReturnStatus.WAREHOUSE_RECEIVED)).isTrue();
        assertThat(ReturnStatus.WAREHOUSE_RECEIVED.canTransitionTo(ReturnStatus.INSPECTED)).isTrue();
        assertThat(ReturnStatus.INSPECTED.canTransitionTo(ReturnStatus.RECOVERY_DECISION)).isTrue();
        assertThat(ReturnStatus.RECOVERY_DECISION.canTransitionTo(ReturnStatus.PARTNER_ASSIGNED)).isTrue();
        assertThat(ReturnStatus.PARTNER_ASSIGNED.canTransitionTo(ReturnStatus.PROCESSING)).isTrue();
        assertThat(ReturnStatus.PROCESSING.canTransitionTo(ReturnStatus.RECOVERED)).isTrue();
        assertThat(ReturnStatus.RECOVERED.canTransitionTo(ReturnStatus.COMPLETED)).isTrue();
    }

    @Test
    @DisplayName("a return cannot skip a stage")
    void cannotSkipStages() {
        assertThat(ReturnStatus.RETURN_CREATED.canTransitionTo(ReturnStatus.INSPECTED)).isFalse();
        assertThat(ReturnStatus.RETURN_CREATED.canTransitionTo(ReturnStatus.COMPLETED)).isFalse();
        assertThat(ReturnStatus.COLLECTED.canTransitionTo(ReturnStatus.PARTNER_ASSIGNED)).isFalse();
        assertThat(ReturnStatus.INSPECTED.canTransitionTo(ReturnStatus.RECOVERED)).isFalse();
    }

    @Test
    @DisplayName("terminal statuses accept no further transitions")
    void terminalStatuses() {
        assertThat(ReturnStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(ReturnStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(ReturnStatus.COMPLETED.allowedTransitions()).isEmpty();
        assertThat(ReturnStatus.CANCELLED.allowedTransitions()).isEmpty();
    }

    @Test
    @DisplayName("a return can be cancelled from any non-terminal stage")
    void cancellable() {
        for (ReturnStatus status : ReturnStatus.values()) {
            if (status.isTerminal()) {
                assertThat(status.canTransitionTo(ReturnStatus.CANCELLED))
                        .as("%s is terminal and cannot be cancelled", status)
                        .isFalse();
            } else {
                assertThat(status.canTransitionTo(ReturnStatus.CANCELLED))
                        .as("%s should be cancellable", status)
                        .isTrue();
            }
        }
    }

    @Test
    @DisplayName("a return cannot move backwards through the lifecycle")
    void noBackwardsTransitions() {
        assertThat(ReturnStatus.WAREHOUSE_RECEIVED.canTransitionTo(ReturnStatus.COLLECTED)).isFalse();
        assertThat(ReturnStatus.RECOVERED.canTransitionTo(ReturnStatus.PROCESSING)).isFalse();
        assertThat(ReturnStatus.COMPLETED.canTransitionTo(ReturnStatus.RECOVERED)).isFalse();
    }
}
