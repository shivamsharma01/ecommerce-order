package com.mcart.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record InventoryAdjustmentItem(
        @NotBlank String productId,
        @Min(1) int quantity
) {
}

