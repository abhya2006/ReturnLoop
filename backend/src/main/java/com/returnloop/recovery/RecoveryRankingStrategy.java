package com.returnloop.recovery;

import java.util.List;

/**
 * Ranks feasible recovery options.
 *
 * <p>Kept as an interface so that a learned model can be introduced later
 * without rewriting the engine: a {@code ModelRecoveryRankingStrategy}
 * implementing this contract could be swapped in via configuration.
 */
public interface RecoveryRankingStrategy {

    /**
     * @param options  all candidate options, eligible or not
     * @param context  the inputs the options were derived from
     * @return the options ordered best first
     */
    List<RecoveryOption> rank(List<RecoveryOption> options, RecoveryContext context);

    /** Human readable identifier surfaced in the API so the operator knows what ranked the list. */
    String name();
}
