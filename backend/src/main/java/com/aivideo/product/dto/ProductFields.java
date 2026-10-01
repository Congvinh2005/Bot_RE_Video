package com.aivideo.product.dto;

import java.util.List;

/** Dữ liệu sản phẩm user nhập tay. Tất cả optional — cho phép nhập từng phần. */
public record ProductFields(
        String name,
        String category,
        String description,
        List<String> features,
        String material,
        String color,
        String size,
        String targetAudience,
        List<String> sellingPoints,
        String price
) {
    public boolean hasAnyValue() {
        return (name != null && !name.isBlank())
                || (category != null && !category.isBlank())
                || (description != null && !description.isBlank())
                || (features != null && !features.isEmpty())
                || (material != null && !material.isBlank())
                || (color != null && !color.isBlank())
                || (size != null && !size.isBlank())
                || (targetAudience != null && !targetAudience.isBlank())
                || (sellingPoints != null && !sellingPoints.isEmpty())
                || (price != null && !price.isBlank());
    }
}
