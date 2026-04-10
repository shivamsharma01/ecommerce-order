package com.mcart.order.dto;

public record OrderItemResponse(
        String productId,
        int quantity,
        long unitPrice,
        long lineTotal
) {
}

