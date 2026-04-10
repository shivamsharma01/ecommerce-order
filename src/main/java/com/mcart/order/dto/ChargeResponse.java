package com.mcart.order.dto;

public record ChargeResponse(
        String status,
        String paymentId
) {
}

