package com.mcart.order.dto;

import java.util.List;

public record CheckoutResponse(
        String orderId,
        String status,
        long totalAmount,
        ShippingAddress shippingAddress,
        List<OrderItemResponse> items
) {
}

