package com.returnloop.recovery;

import com.returnloop.returns.ProductCondition;

import java.math.BigDecimal;

/**
 * The inputs the recovery engine reasons over.
 *
 * @param condition        inspected condition of the goods
 * @param resaleValue      expected resale value if the goods are made sellable
 * @param originalPrice    original retail price, used for depreciation floors
 * @param weightKg         weight of the goods, drives transport and disposal costs
 * @param accessoriesComplete whether all accessories were present at inspection
 * @param functional       whether the product powers on / passes a functional test
 * @param category         product category, drives which partner types qualify
 * @param region           the geographic region, used for partner and transport matching
 */
public record RecoveryContext(
        ProductCondition condition,
        BigDecimal resaleValue,
        BigDecimal originalPrice,
        BigDecimal weightKg,
        boolean accessoriesComplete,
        boolean functional,
        String category,
        String region) {

    public BigDecimal resaleValueOrZero() {
        return resaleValue == null ? BigDecimal.ZERO : resaleValue;
    }

    public BigDecimal originalPriceOrZero() {
        return originalPrice == null ? BigDecimal.ZERO : originalPrice;
    }

    public BigDecimal weightOrZero() {
        return weightKg == null ? BigDecimal.ZERO : weightKg;
    }
}
