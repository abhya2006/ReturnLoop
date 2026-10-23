package com.returnloop.recovery;

import com.returnloop.returns.ProductCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedRecoveryRankingStrategyTest {

    private final RuleBasedRecoveryRankingStrategy strategy = new RuleBasedRecoveryRankingStrategy();

    private RecoveryContext context() {
        return new RecoveryContext(ProductCondition.GOOD, new BigDecimal("600.00"), new BigDecimal("900.00"),
                new BigDecimal("2.00"), true, true, "MOBILE_PHONES", "US-NORTHEAST");
    }

    @Test
    @DisplayName("the highest net value option is ranked first")
    void ranksByNetValue() {
        List<RecoveryOption> options = RecoveryCostModel.evaluate(context()).values().stream().toList();
        List<RecoveryOption> ranked = strategy.rank(options, context());

        assertThat(ranked).isNotEmpty();
        RecoveryOption best = ranked.get(0);
        assertThat(best.eligible()).isTrue();
        assertThat(ranked.stream()
                .filter(RecoveryOption::eligible)
                .map(RecoveryOption::netRecoveryValue)
                .max(java.util.Comparator.naturalOrder()))
                .contains(best.netRecoveryValue());
    }

    @Test
    @DisplayName("eligible options always outrank ineligible ones")
    void eligibleOptionsRankFirst() {
        List<RecoveryOption> options = List.of(
                new RecoveryOption(RecoveryPath.SAFE_DISPOSAL, "Safe disposal",
                        new BigDecimal("0.00"), new BigDecimal("10.00"), new BigDecimal("1.00"),
                        new BigDecimal("0.00"), new BigDecimal("6.50"), new BigDecimal("-17.50"),
                        1, 90, "", List.of("not viable"), false, BigDecimal.ZERO, BigDecimal.ZERO),
                new RecoveryOption(RecoveryPath.REPAIR, "Repair",
                        new BigDecimal("600.00"), new BigDecimal("100.00"), new BigDecimal("5.00"),
                        new BigDecimal("70.00"), new BigDecimal("6.50"), new BigDecimal("418.50"),
                        7, 80, "", List.of(), true, new BigDecimal("1.90"), new BigDecimal("16.15")));

        List<RecoveryOption> ranked = strategy.rank(options, context());

        assertThat(ranked.get(0).eligible()).isTrue();
        assertThat(ranked.get(1).eligible()).isFalse();
    }

    @Test
    @DisplayName("the strategy identifies itself so the operator knows what ranked the list")
    void hasName() {
        assertThat(strategy.name()).isEqualTo("rule-based-explainable");
    }
}
