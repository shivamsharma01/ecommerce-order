package com.mcart.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record InventoryAdjustRequest(
        String orderId,
        @NotEmpty List<@Valid InventoryAdjustmentItem> items
) {
}

