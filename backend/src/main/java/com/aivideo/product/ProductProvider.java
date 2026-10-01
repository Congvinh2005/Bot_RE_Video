package com.aivideo.product;

import com.aivideo.product.dto.ProductFields;
import com.aivideo.product.dto.ProductRequest;

/**
 * Abstraction nguồn dữ liệu sản phẩm. Không hard-code scraping website.
 * Provider tương lai (official shop API...) chỉ cần implement interface này.
 */
public interface ProductProvider {

    boolean supports(ProductRequest request);

    ProductContext resolve(ProductRequest request);
}
