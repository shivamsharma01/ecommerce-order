package com.mcart.order.dto;

public record CartItemResponse(
        String productId,
        int quantity
) {
}

