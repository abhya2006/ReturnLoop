package com.returnloop.recovery;

import java.math.BigDecimal;
import java.util.List;

/**
 * A single candidate recovery path with its full economics and rationale.
 *
 * <p>The value model is:
 * {@code netRecoveryValue = expectedResaleValue - repairCost - inspectionCost
 * - handlingCost - transportationCost - partnerFees}
 */
public record RecoveryOption(
        RecoveryPath path,
        String label,
        /** Gross value expected from selling / salvaging the goods. */
        BigDecimal expectedResaleValue,
        /** Cost of the work required to make the goods sellable. */
        BigDecimal processingCost,
        /** Cost of getting the goods to the facility that performs the work. */
        BigDecimal transportationCost,
        /** Fees charged by the partner that will carry out the work. */
        BigDecimal partnerFees,
        /** Cost of the inspection that produced the inputs for this calculation. */
        BigDecimal inspectionCost,
        /** Net = expectedResaleValue - processing - transportation - partnerFees - inspection. */
        BigDecimal netRecoveryValue,
        /** Expected end-to-end processing time in days. */
        int estimatedProcessingDays,
        /** 0-100; how confident the engine is in the underlying inputs. */
        int dataQualityScore,
        /** Plain-language explanation of why this option scored as it did. */
        String explanation,
        /** Reasons this option was ruled out, empty when eligible. */
        List<String> restrictions,
        /** True when the option may be selected. */
        boolean eligible,
        /** Estimated kg of waste diverted from landfill, where an assumption applies. */
        BigDecimal wasteDivertedKg,
        /** Estimated kg CO2e avoided relative to disposal, where an assumption applies. */
        BigDecimal carbonAvoidedKg) {
}
