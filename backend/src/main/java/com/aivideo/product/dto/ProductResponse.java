package com.aivideo.product.dto;

import com.aivideo.product.Product;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID projectId,
        String name,
        String category,
        String description,
        List<String> features,
        String material,
        String color,
        String size,
        String targetAudience,
        List<String> sellingPoints,
        String price,
        String productUrl,
        Instant createdAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getProject().getId(),
                product.getName(),
                product.getCategory(),
                product.getDescription(),
                product.getFeatures(),
                product.getMaterial(),
                product.getColor(),
                product.getSize(),
                product.getTargetAudience(),
                product.getSellingPoints(),
                product.getPrice(),
                product.getProductUrl(),
                product.getCreatedAt());
    }
}
