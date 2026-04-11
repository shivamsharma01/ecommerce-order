package com.mcart.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductResponse(
        String id,
        String name,
        Double price,
        List<ProductGalleryImage> gallery
) {
}

