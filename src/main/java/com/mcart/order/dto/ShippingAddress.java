package com.mcart.order.dto;

public record ShippingAddress(
        String fullName,
        String phone,
        String line1,
        String line2,
        String city,
        String state,
        String pincode,
        String country
) {
}

