package com.returnloop.recovery;

import com.returnloop.returns.ProductCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The recovery economics are the product's core claim, so the arithmetic and
 * the eligibility rules are tested directly.
 */
class RecoveryCostModelTest {

    private static final BigDecimal RESALE = new BigDecimal("600.00");
    private static final BigDecimal WEIGHT = new BigDecimal("2.00");

    private RecoveryContext context(ProductCondition condition, boolean functional, boolean accessories) {
        return new RecoveryContext(condition, RESALE, new BigDecimal("900.00"), WEIGHT, accessories, functional,
                "MOBILE_PHONES", "US-NORTHEAST");
    }

    @Test
    @DisplayName("net recovery value equals gross value less every cost")
    void netValueArithmetic() {
        RecoveryContext context = context(ProductCondition.GOOD, true, true);
        Map<RecoveryPath, RecoveryOption> options = RecoveryCostModel.evaluate(context);

        RecoveryOption repair = options.get(RecoveryPath.REPAIR);
        BigDecimal expected = repair.expectedResaleValue()
                .subtract(repair.processingCost())
                .subtract(repair.transportationCost())
                .subtract(repair.partnerFees())
                .subtract(repair.inspectionCost());
        assertThat(repair.netRecoveryValue()).isEqualByComparingTo(expected.setScale(2, java.math.RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("a heavier product costs more to transport and handle")
    void transportScalesWithWeight() {
        RecoveryOption light = RecoveryCostModel.evaluate(context(ProductCondition.GOOD, true, true))
                .get(RecoveryPath.LIQUIDATION);
        RecoveryOption heavy = RecoveryCostModel.evaluate(
                        new RecoveryContext(ProductCondition.GOOD, RESALE, new BigDecimal("900.00"),
                                new BigDecimal("40.00"), true, true, "MOBILE_PHONES", "US-NORTHEAST"))
                .get(RecoveryPath.LIQUIDATION);

        assertThat(heavy.transportationCost()).isGreaterThan(light.transportationCost());
    }

    @Test
    @DisplayName("restocking as new is only eligible for sealed, working goods")
    void restockEligibility() {
        assertThat(RecoveryCostModel.evaluate(context(ProductCondition.NEW, true, true))
                .get(RecoveryPath.RESTOCK_AS_NEW).eligible()).isTrue();
        assertThat(RecoveryCostModel.evaluate(context(ProductCondition.LIKE_NEW, true, true))
                .get(RecoveryPath.RESTOCK_AS_NEW).eligible()).isFalse();
        assertThat(RecoveryCostModel.evaluate(context(ProductCondition.NEW, false, true))
                .get(RecoveryPath.RESTOCK_AS_NEW).eligible()).isFalse();
        assertThat(RecoveryCostModel.evaluate(context(ProductCondition.NEW, true, false))
                .get(RecoveryPath.RESTOCK_AS_NEW).eligible()).isFalse();
    }

    @Test
    @DisplayName("an unsafe product cannot enter the refurbishment stream")
    void unsafeIsExcludedFromRefurbishment() {
        RecoveryOption option = RecoveryCostModel.evaluate(context(ProductCondition.UNSAFE, false, true))
                .get(RecoveryPath.REFURBISHMENT);
        assertThat(option.eligible()).isFalse();
        assertThat(option.restrictions()).isNotEmpty();
    }

    @Test
    @DisplayName("only functional goods can be liquidated")
    void liquidationRequiresFunctionality() {
        assertThat(RecoveryCostModel.evaluate(context(ProductCondition.GOOD, true, true))
                .get(RecoveryPath.LIQUIDATION).eligible()).isTrue();
        assertThat(RecoveryCostModel.evaluate(context(ProductCondition.DAMAGED, false, true))
                .get(RecoveryPath.LIQUIDATION).eligible()).isFalse();
    }

    @Test
    @DisplayName("safe disposal is always available and never diverts waste")
    void disposalIsAlwaysAvailable() {
        RecoveryOption option = RecoveryCostModel.evaluate(context(ProductCondition.UNSAFE, false, false))
                .get(RecoveryPath.SAFE_DISPOSAL);
        assertThat(option.eligible()).isTrue();
        assertThat(option.wasteDivertedKg()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(option.carbonAvoidedKg()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("an ineligible option explains why it was ruled out")
    void ineligibleOptionsCarryRestrictions() {
        RecoveryOption option = RecoveryCostModel.evaluate(context(ProductCondition.DAMAGED, false, true))
                .get(RecoveryPath.OPEN_BOX_RESALE);
        assertThat(option.eligible()).isFalse();
        assertThat(option.restrictions()).isNotEmpty();
        assertThat(option.explanation()).contains("not currently eligible");
    }
}
