package com.mcart.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserAddressResponse(
        UUID addressId,
        String fullName,
        String phone,
        String line1,
        String line2,
        String city,
        String state,
        String pincode,
        String country,
        Boolean isDefault
) {
}

