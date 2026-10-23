package com.returnloop.returns;

import java.math.BigDecimal;
import java.util.UUID;

public record ReturnItemResponse(
        UUID id,
        UUID productId,
        String sku,
        String productName,
        int quantity,
        ProductCondition condition,
        String conditionLabel,
        String serialNumber,
        boolean accessoriesComplete,
        BigDecimal estimatedValue,
        String notes) {

    public static ReturnItemResponse from(ReturnItem item) {
        return new ReturnItemResponse(
                item.getId(),
                item.getProductId(),
                item.getSku(),
                item.getProductName(),
                item.getQuantity(),
                item.getCondition(),
                item.getCondition().getLabel(),
                item.getSerialNumber(),
                item.isAccessoriesComplete(),
                item.getEstimatedValue(),
                item.getNotes());
    }
}
