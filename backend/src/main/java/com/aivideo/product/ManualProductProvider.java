package com.aivideo.product;

import com.aivideo.product.dto.ProductFields;
import com.aivideo.product.dto.ProductRequest;
import org.springframework.stereotype.Component;

/** Provider mặc định: dùng dữ liệu user nhập tay. Luôn chạy được, không phụ thuộc ngoài. */
@Component
public class ManualProductProvider implements ProductProvider {

    @Override
    public boolean supports(ProductRequest request) {
        ProductFields fields = request.product();
        return fields != null && fields.hasAnyValue();
    }

    @Override
    public ProductContext resolve(ProductRequest request) {
        ProductFields fields = request.product();
        return new ProductContext(
                fields.name(),
                fields.category(),
                fields.description(),
                fields.features(),
                fields.material(),
                fields.color(),
                fields.size(),
                fields.targetAudience(),
                fields.sellingPoints(),
                fields.price(),
                request.productUrl());
    }
}
