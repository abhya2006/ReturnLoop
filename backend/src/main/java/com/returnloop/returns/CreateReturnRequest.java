package com.returnloop.returns;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateReturnRequest(
        @NotNull(message = "reason is required") ReturnReason reason,
        @Size(max = 120) String sourceReference,
        @Size(max = 160) String customerName,
        @Size(max = 255) String customerEmail,
        @Size(max = 500) String originAddress,
        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") BigDecimal originLatitude,
        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") BigDecimal originLongitude,
        UUID warehouseId,
        @Size(max = 2000) String notes,
        @NotEmpty(message = "at least one item is required") @Valid List<CreateReturnItem> items) {
}
