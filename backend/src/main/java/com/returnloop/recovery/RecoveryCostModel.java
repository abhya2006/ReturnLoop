package com.returnloop.recovery;

import com.returnloop.returns.ProductCondition;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The cost and value assumptions behind the recovery engine.
 *
 * <p>All coefficients are explicit and configurable so that an operator can see
 * exactly how a net recovery figure was derived. Coefficients are expressed as
 * fractions of the product's resale value unless stated otherwise.
 */
public final class RecoveryCostModel {

    /** Flat inspection cost per unit, in the organisation's currency. */
    public static final BigDecimal INSPECTION_COST = new BigDecimal("6.50");

    /** Handling cost per kilogram, covering warehousing and movement. */
    public static final BigDecimal HANDLING_COST_PER_KG = new BigDecimal("1.20");

    /** Transport cost per kilogram per 100 km, a synthetic rate card. */
    public static final BigDecimal TRANSPORT_COST_PER_KG_PER_100KM = new BigDecimal("0.45");

    /** Average collection radius in km used for the transport estimate. */
    public static final BigDecimal AVERAGE_TRANSPORT_KM = new BigDecimal("45");

    /** Fee a partner charges as a fraction of the gross value it realises. */
    public static final BigDecimal PARTNER_FEE_RATE = new BigDecimal("0.12");

    /** Value retained when selling an open-box unit. */
    public static final BigDecimal OPEN_BOX_VALUE_RATE = new BigDecimal("0.75");

    /** Value retained by a liquidation partner. */
    public static final BigDecimal LIQUIDATION_VALUE_RATE = new BigDecimal("0.55");

    /** Value recovered by harvesting spare parts. */
    public static final BigDecimal SPARE_PARTS_VALUE_RATE = new BigDecimal("0.18");

    /** Value recovered through material recycling. */
    public static final BigDecimal RECYCLING_VALUE_RATE = new BigDecimal("0.04");

    /** Value recovered from safe disposal. */
    public static final BigDecimal DISPOSAL_VALUE_RATE = new BigDecimal("0.00");

    /** Cost of disposal per kilogram. */
    public static final BigDecimal DISPOSAL_COST_PER_KG = new BigDecimal("0.80");

    /** Repair work cost as a fraction of resale value, for a repairable product. */
    public static final BigDecimal REPAIR_COST_RATE = new BigDecimal("0.18");

    /** Refurbishment work cost as a fraction of resale value. */
    public static final BigDecimal REFURBISH_COST_RATE = new BigDecimal("0.30");

    /** Additional cost when accessories are missing. */
    public static final BigDecimal MISSING_ACCESSORIES_COST = new BigDecimal("12.00");

    /** Relative CO2e factor (kg CO2e per kg) for electronics, used for estimates. */
    public static final BigDecimal ELECTRONICS_CO2E_PER_KG = new BigDecimal("8.50");

    /** Relative CO2e factor for generic goods. */
    public static final BigDecimal GENERIC_CO2E_PER_KG = new BigDecimal("2.10");

    public RecoveryCostModel() {
    }

    /**
     * Applies the cost model to produce every candidate recovery option for a
     * given context.
     */
    public static Map<RecoveryPath, RecoveryOption> evaluate(RecoveryContext context) {
        Map<RecoveryPath, RecoveryOption> options = new LinkedHashMap<>();
        BigDecimal resale = context.resaleValueOrZero();
        BigDecimal weight = context.weightOrZero();
        BigDecimal transportation = transportCost(weight);
        BigDecimal handling = handlingCost(weight);
        ProductCondition condition = context.condition() == null ? ProductCondition.GOOD : context.condition();

        options.put(RecoveryPath.RESTOCK_AS_NEW, build(RecoveryPath.RESTOCK_AS_NEW, context, condition,
                resale, BigDecimal.ZERO, transportation, handling, 0, 100));
        options.put(RecoveryPath.OPEN_BOX_RESALE, build(RecoveryPath.OPEN_BOX_RESALE, context, condition,
                resale.multiply(OPEN_BOX_VALUE_RATE), BigDecimal.ZERO, transportation, handling, 3, 90));
        options.put(RecoveryPath.REPAIR, build(RecoveryPath.REPAIR, context, condition,
                resale, resale.multiply(REPAIR_COST_RATE), transportation, handling, 7, 75));
        options.put(RecoveryPath.REFURBISHMENT, build(RecoveryPath.REFURBISHMENT, context, condition,
                resale, resale.multiply(REFURBISH_COST_RATE), transportation, handling, 12, 70));
        options.put(RecoveryPath.LIQUIDATION, build(RecoveryPath.LIQUIDATION, context, condition,
                resale.multiply(LIQUIDATION_VALUE_RATE), BigDecimal.ZERO, transportation, handling, 5, 80));
        options.put(RecoveryPath.SPARE_PARTS, build(RecoveryPath.SPARE_PARTS, context, condition,
                resale.multiply(SPARE_PARTS_VALUE_RATE), BigDecimal.ZERO, transportation, handling, 10, 60));
        options.put(RecoveryPath.RECYCLING, build(RecoveryPath.RECYCLING, context, condition,
                resale.multiply(RECYCLING_VALUE_RATE), BigDecimal.ZERO, transportation, handling, 14, 55));
        options.put(RecoveryPath.SAFE_DISPOSAL, build(RecoveryPath.SAFE_DISPOSAL, context, condition,
                resale.multiply(DISPOSAL_VALUE_RATE), weight.multiply(DISPOSAL_COST_PER_KG),
                transportation, handling, 1, 85));
        return options;
    }

    private static RecoveryOption build(RecoveryPath path, RecoveryContext context, ProductCondition condition,
                                        BigDecimal grossValue, BigDecimal workCost, BigDecimal transportation,
                                        BigDecimal handling, int processingDays, int baseQuality) {
        BigDecimal extra = context.accessoriesComplete() ? BigDecimal.ZERO : MISSING_ACCESSORIES_COST;
        BigDecimal processingCost = workCost.add(extra);
        BigDecimal partnerFees = grossValue.multiply(PARTNER_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCosts = processingCost.add(transportation).add(handling).add(partnerFees).add(INSPECTION_COST);
        BigDecimal net = grossValue.subtract(totalCosts).setScale(2, RoundingMode.HALF_UP);

        boolean eligible = isEligible(path, context, condition);
        BigDecimal diversion = eligible ? wasteDivertedKg(path, context, weightOf(context)) : BigDecimal.ZERO;
        BigDecimal carbon = eligible
                ? diversion.multiply(carbonFactor(context)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        int quality = adjustQuality(baseQuality, context, eligible);

        return new RecoveryOption(
                path,
                path.getLabel(),
                grossValue.setScale(2, RoundingMode.HALF_UP),
                processingCost.setScale(2, RoundingMode.HALF_UP),
                transportation.setScale(2, RoundingMode.HALF_UP),
                partnerFees,
                INSPECTION_COST,
                net,
                processingDays,
                quality,
                explain(path, context, condition, grossValue, totalCosts, net, eligible),
                eligible ? java.util.List.of() : restrictions(path, context, condition),
                eligible,
                diversion.setScale(3, RoundingMode.HALF_UP),
                carbon);
    }

    /**
     * Eligibility rules. These encode the business rules that decide which
     * recovery destinations are permitted for a given product state.
     */
    static boolean isEligible(RecoveryPath path, RecoveryContext context, ProductCondition condition) {
        return switch (path) {
            case RESTOCK_AS_NEW -> condition == ProductCondition.NEW
                    && context.functional() && context.accessoriesComplete();
            case OPEN_BOX_RESALE -> condition == ProductCondition.LIKE_NEW
                    || condition == ProductCondition.GOOD || condition == ProductCondition.NEW;
            case REPAIR -> context.functional() || condition == ProductCondition.DAMAGED
                    || condition == ProductCondition.FAIR;
            case REFURBISHMENT -> condition != ProductCondition.UNSAFE
                    && condition != ProductCondition.UNSELLABLE;
            case LIQUIDATION -> context.functional()
                    && (condition == ProductCondition.GOOD || condition == ProductCondition.FAIR
                    || condition == ProductCondition.LIKE_NEW);
            case SPARE_PARTS -> condition == ProductCondition.DAMAGED
                    || condition == ProductCondition.UNSELLABLE || condition == ProductCondition.UNSAFE;
            case RECYCLING -> condition == ProductCondition.UNSELLABLE
                    || condition == ProductCondition.DAMAGED || condition == ProductCondition.UNSAFE
                    || condition == ProductCondition.FAIR;
            case SAFE_DISPOSAL -> true;
        };
    }

    private static java.util.List<String> restrictions(RecoveryPath path, RecoveryContext context,
                                                       ProductCondition condition) {
        java.util.List<String> reasons = new java.util.ArrayList<>();
        switch (path) {
            case RESTOCK_AS_NEW -> {
                if (condition != ProductCondition.NEW) {
                    reasons.add("Product is not in new, sealed condition");
                }
                if (!context.functional()) {
                    reasons.add("Product did not pass the functionality test");
                }
                if (!context.accessoriesComplete()) {
                    reasons.add("Accessories are missing");
                }
            }
            case OPEN_BOX_RESALE -> reasons.add("Condition " + condition.getLabel()
                    + " does not meet the open-box resale threshold");
            case REPAIR -> {
                if (!context.functional() && condition == ProductCondition.UNSAFE) {
                    reasons.add("Product was flagged unsafe; repair is not permitted");
                }
                if (condition == ProductCondition.UNSELLABLE) {
                    reasons.add("Product was assessed as unsellable");
                }
            }
            case REFURBISHMENT -> {
                if (condition == ProductCondition.UNSAFE) {
                    reasons.add("Unsafe products cannot enter the refurbishment stream");
                }
                if (condition == ProductCondition.UNSELLABLE) {
                    reasons.add("Product was assessed as unsellable");
                }
            }
            case LIQUIDATION -> {
                if (!context.functional()) {
                    reasons.add("Only functional goods can be liquidated");
                }
                reasons.add("Condition " + condition.getLabel() + " is below the liquidation threshold");
            }
            case SPARE_PARTS -> reasons.add("Condition " + condition.getLabel()
                    + " does not warrant a parts recovery");
            case RECYCLING -> reasons.add("Condition " + condition.getLabel()
                    + " is above the recycling threshold");
            case SAFE_DISPOSAL -> {
            }
        }
        return reasons;
    }

    private static String explain(RecoveryPath path, RecoveryContext context, ProductCondition condition,
                                  BigDecimal gross, BigDecimal costs, BigDecimal net, boolean eligible) {
        String base = switch (path) {
            case RESTOCK_AS_NEW -> "Returned to sellable stock with no rework required";
            case OPEN_BOX_RESALE -> "Sold as open-box at a reduced price, avoiding rework cost";
            case REPAIR -> "Repaired to a working state and resold";
            case REFURBISHMENT -> "Fully refurbished and resold with a warranty";
            case LIQUIDATION -> "Wholesaled in bulk to a liquidation partner";
            case SPARE_PARTS -> "Components harvested for parts reuse";
            case RECYCLING -> "Materials recovered through a recycling stream";
            case SAFE_DISPOSAL -> "Disposed of safely, the least value-destructive option";
        };
        String economics = "Expected gross value " + gross.setScale(2, RoundingMode.HALF_UP)
                + " less total costs " + costs.setScale(2, RoundingMode.HALF_UP)
                + " gives a net recovery value of " + net.setScale(2, RoundingMode.HALF_UP) + ".";
        String conditionNote = condition == null ? "" : " Assessed condition: " + condition.getLabel() + ".";
        String eligibility = eligible ? "" : " This option is not currently eligible: "
                + String.join("; ", restrictions(path, context, condition)) + ".";
        return base + "." + conditionNote + " " + economics + eligibility;
    }

    private static int adjustQuality(int baseQuality, RecoveryContext context, boolean eligible) {
        int quality = baseQuality;
        if (!eligible) {
            quality -= 15;
        }
        if (context.resaleValueOrZero().signum() == 0) {
            quality -= 25;
        }
        if (context.weightOrZero().signum() == 0) {
            quality -= 5;
        }
        return Math.max(5, Math.min(100, quality));
    }

    private static BigDecimal weightOf(RecoveryContext context) {
        return context.weightOrZero();
    }

    private static BigDecimal transportCost(BigDecimal weightKg) {
        return weightKg.multiply(TRANSPORT_COST_PER_KG_PER_100KM)
                .multiply(AVERAGE_TRANSPORT_KM)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal handlingCost(BigDecimal weightKg) {
        return weightKg.multiply(HANDLING_COST_PER_KG).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal wasteDivertedKg(RecoveryPath path, RecoveryContext context, BigDecimal weightKg) {
        if (path == RecoveryPath.SAFE_DISPOSAL) {
            return BigDecimal.ZERO;
        }
        BigDecimal recoveryRate = switch (path) {
            case RESTOCK_AS_NEW, OPEN_BOX_RESALE, REPAIR, REFURBISHMENT, LIQUIDATION -> new BigDecimal("0.95");
            case SPARE_PARTS -> new BigDecimal("0.60");
            case RECYCLING -> new BigDecimal("0.35");
            case SAFE_DISPOSAL -> BigDecimal.ZERO;
        };
        return weightKg.multiply(recoveryRate).setScale(3, RoundingMode.HALF_UP);
    }

    private static BigDecimal carbonFactor(RecoveryContext context) {
        String category = context.category() == null ? "" : context.category().toUpperCase();
        if (category.contains("ELECTRON") || category.contains("MOBILE") || category.contains("LAPTOP")
                || category.contains("APPLIANCE")) {
            return ELECTRONICS_CO2E_PER_KG;
        }
        return GENERIC_CO2E_PER_KG;
    }
}
