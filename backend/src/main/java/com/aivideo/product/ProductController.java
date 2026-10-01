package com.aivideo.product;

import com.aivideo.product.dto.ProductRequest;
import com.aivideo.product.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{id}")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping("/product")
    public ResponseEntity<ProductResponse> upsertProduct(
            @PathVariable UUID id,
            @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.upsertProduct(id, request));
    }
}
