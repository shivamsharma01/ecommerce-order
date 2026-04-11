package com.mcart.order.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderItemResponse(
        String productId,
        int quantity,
        long unitPrice,
        long lineTotal,
        String productName,
        String thumbnailUrl
) {
}

