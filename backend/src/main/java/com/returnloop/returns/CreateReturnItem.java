package com.returnloop.returns;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReturnItem(
        @NotBlank(message = "sku is required") String sku,
        @Min(value = 1, message = "quantity must be at least 1")
        @Max(value = 999, message = "quantity must not exceed 999") Integer quantity,
        ProductCondition condition,
        @Size(max = 120) String serialNumber,
        Boolean accessoriesComplete,
        @Size(max = 1000) String notes) {
}
