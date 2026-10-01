package com.aivideo.product;

import com.aivideo.common.exception.BadRequestException;
import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.product.dto.ProductRequest;
import com.aivideo.product.dto.ProductResponse;
import com.aivideo.project.Project;
import com.aivideo.project.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProjectRepository projectRepository;
    private final ProductRepository productRepository;
    private final List<ProductProvider> productProviders;

    @Transactional
    public ProductResponse upsertProduct(UUID projectId, ProductRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));
        if (request == null || request.isEmpty()) {
            throw new BadRequestException("PRODUCT_INPUT_REQUIRED",
                    "Provide productUrl or manual product data");
        }
        ProductContext context = productProviders.stream()
                .filter(p -> p.supports(request))
                .findFirst()
                .map(p -> p.resolve(request))
                .orElseGet(() -> emptyContextWithUrl(request.productUrl()));

        Product product = productRepository.findByProjectId(projectId)
                .map(existing -> update(existing, context))
                .orElseGet(() -> create(project, context));
        Product saved = productRepository.save(product);
        log.info("PRODUCT_UPSERT project={} product={}", projectId, saved.getId());
        return ProductResponse.from(saved);
    }

    private ProductContext emptyContextWithUrl(String productUrl) {
        return new ProductContext(null, null, null, null, null, null,
                null, null, null, null, productUrl);
    }

    private Product create(Project project, ProductContext context) {
        Product product = new Product();
        product.setProject(project);
        return update(product, context);
    }

    private Product update(Product product, ProductContext context) {
        product.setName(context.name());
        product.setCategory(context.category());
        product.setDescription(context.description());
        product.setFeatures(context.features());
        product.setMaterial(context.material());
        product.setColor(context.color());
        product.setSize(context.size());
        product.setTargetAudience(context.targetAudience());
        product.setSellingPoints(context.sellingPoints());
        product.setPrice(context.price());
        product.setProductUrl(context.productUrl());
        return product;
    }
}
