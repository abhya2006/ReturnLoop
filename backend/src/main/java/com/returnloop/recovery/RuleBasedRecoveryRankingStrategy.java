package com.returnloop.recovery;

import com.returnloop.common.error.ApiException;
import com.returnloop.common.error.ErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * The default, explainable ranking strategy.
 *
 * <p>Options are ordered by net recovery value, with two deliberate adjustments:
 * <ul>
 *   <li>options that also divert waste are preferred over equally valuable
 *       options that do not, because the platform's purpose is circularity;</li>
 *   <li>the first feasible option is reported as the recommendation and the
 *       remainder as alternatives.</li>
 * </ul>
 *
 * <p>Every ordering decision is expressed as a weighted score so the reasoning
 * can be shown to an operator.
 */
@Component
@ConditionalOnProperty(name = "returnloop.recovery.strategy", havingValue = "rule-based", matchIfMissing = true)
public class RuleBasedRecoveryRankingStrategy implements RecoveryRankingStrategy {

    /** Weight applied to the net recovery value when scoring. */
    private static final BigDecimal VALUE_WEIGHT = new BigDecimal("1.0");

    /** Weight applied to the fraction of net value retained relative to resale value. */
    private static final BigDecimal YIELD_WEIGHT = new BigDecimal("0.25");

    /** Weight applied to turnaround time in days; shorter is better. */
    private static final BigDecimal SPEED_WEIGHT = new BigDecimal("0.5");

    /** Weight applied to the waste diversion benefit. */
    private static final BigDecimal CIRCULARITY_WEIGHT = new BigDecimal("0.15");

    /** Weight applied to the data quality score. */
    private static final BigDecimal CONFIDENCE_WEIGHT = new BigDecimal("0.1");

    @Override
    public List<RecoveryOption> rank(List<RecoveryOption> options, RecoveryContext context) {
        if (options == null || options.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "No recovery options were supplied for ranking");
        }
        BigDecimal maxNet = options.stream()
                .map(RecoveryOption::netRecoveryValue)
                .filter(v -> v != null && v.signum() > 0)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ONE);
        if (maxNet.signum() == 0) {
            maxNet = BigDecimal.ONE;
        }
        BigDecimal resaleAnchor = context.resaleValueOrZero();
        if (resaleAnchor.signum() == 0) {
            resaleAnchor = BigDecimal.ONE;
        }
        int maxDays = options.stream()
                .mapToInt(RecoveryOption::estimatedProcessingDays)
                .max()
                .orElse(1);
        if (maxDays <= 0) {
            maxDays = 1;
        }
        BigDecimal maxDiversion = options.stream()
                .map(RecoveryOption::wasteDivertedKg)
                .filter(v -> v != null)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        if (maxDiversion.signum() == 0) {
            maxDiversion = BigDecimal.ONE;
        }

        final BigDecimal scoreMaxNet = maxNet;
        final BigDecimal scoreResaleAnchor = resaleAnchor;
        final int scoreMaxDays = maxDays;
        final BigDecimal scoreMaxDiversion = maxDiversion;

        BigDecimal valueWeight = VALUE_WEIGHT;
        BigDecimal yieldWeight = YIELD_WEIGHT;
        BigDecimal speedWeight = SPEED_WEIGHT;
        BigDecimal circularityWeight = CIRCULARITY_WEIGHT;
        BigDecimal confidenceWeight = CONFIDENCE_WEIGHT;

        return options.stream()
                .sorted(Comparator.comparing((RecoveryOption option) -> score(option, scoreMaxNet, scoreResaleAnchor, scoreMaxDays,
                                scoreMaxDiversion, valueWeight, yieldWeight, speedWeight, circularityWeight, confidenceWeight))
                        .reversed()
                        .thenComparing(Comparator.comparing(option -> option.eligible() ? 0 : 1)))
                .toList();
    }

    private BigDecimal score(RecoveryOption option, BigDecimal maxNet, BigDecimal resaleAnchor, int maxDays,
                             BigDecimal maxDiversion, BigDecimal valueWeight, BigDecimal yieldWeight,
                             BigDecimal speedWeight, BigDecimal circularityWeight, BigDecimal confidenceWeight) {
        BigDecimal valueScore = option.netRecoveryValue().max(BigDecimal.ZERO)
                .divide(maxNet, 6, java.math.RoundingMode.HALF_UP);

        BigDecimal net = option.netRecoveryValue();
        BigDecimal yieldScore = net.signum() <= 0 ? BigDecimal.ZERO
                : net.divide(resaleAnchor, 6, java.math.RoundingMode.HALF_UP).min(BigDecimal.ONE);

        BigDecimal speedScore = BigDecimal.valueOf(maxDays - option.estimatedProcessingDays())
                .divide(BigDecimal.valueOf(maxDays), 6, java.math.RoundingMode.HALF_UP);

        BigDecimal diversion = option.wasteDivertedKg() == null ? BigDecimal.ZERO : option.wasteDivertedKg();
        BigDecimal circularityScore = diversion.divide(maxDiversion, 6, java.math.RoundingMode.HALF_UP);

        BigDecimal confidenceScore = BigDecimal.valueOf(option.dataQualityScore())
                .divide(BigDecimal.valueOf(100), 6, java.math.RoundingMode.HALF_UP);

        BigDecimal score = valueScore.multiply(valueWeight)
                .add(yieldScore.multiply(yieldWeight))
                .add(speedScore.multiply(speedWeight))
                .add(circularityScore.multiply(circularityWeight))
                .add(confidenceScore.multiply(confidenceWeight));

        // Ineligible options always rank below every eligible option.
        return option.eligible() ? score : score.subtract(new BigDecimal("100"));
    }

    @Override
    public String name() {
        return "rule-based-explainable";
    }
}
