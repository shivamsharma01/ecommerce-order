package com.mcart.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ChargeRequest(
        @NotBlank String orderId,
        @Min(1) long amount
) {
}

