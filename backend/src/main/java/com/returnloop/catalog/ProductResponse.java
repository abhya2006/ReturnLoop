package com.returnloop.catalog;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String sku,
        String name,
        String brand,
        String model,
        ProductCategory category,
        String description,
        String imageUrl,
        BigDecimal weightKg,
        BigDecimal originalPrice,
        BigDecimal resaleValue,
        boolean active) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getBrand(),
                product.getModel(),
                product.getCategory(),
                product.getDescription(),
                product.getImageUrl(),
                product.getWeightKg(),
                product.getOriginalPrice(),
                product.getResaleValue(),
                product.isActive());
    }
}
