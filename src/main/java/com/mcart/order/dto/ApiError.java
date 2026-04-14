package com.mcart.order.dto;

public record ApiError(
        String code,
        String message
) {
}

