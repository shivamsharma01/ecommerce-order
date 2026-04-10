package com.mcart.order.dto;

import java.time.Instant;
import java.util.List;

public record OrderSummaryResponse(
        String orderId,
        String status,
        long totalAmount,
        Instant createdAt,
        List<OrderItemResponse> items
) {
}

