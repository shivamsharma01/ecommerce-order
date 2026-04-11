package com.mcart.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductGalleryImage(String thumbnailUrl, String hdUrl, String alt) {
}
