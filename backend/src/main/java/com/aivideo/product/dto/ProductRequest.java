package com.aivideo.product.dto;

public record ProductRequest(
        String productUrl,
        ProductFields product
) {
    public boolean isEmpty() {
        boolean noUrl = productUrl == null || productUrl.isBlank();
        boolean noFields = product == null || !product.hasAnyValue();
        return noUrl && noFields;
    }
}
