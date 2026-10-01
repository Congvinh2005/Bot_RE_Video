package com.aivideo.product;

import java.util.List;

/** Context sản phẩm chuẩn mà AI downstream sử dụng. */
public record ProductContext(
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
        String productUrl
) {
}
