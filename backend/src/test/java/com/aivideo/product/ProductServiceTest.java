package com.aivideo.product;

import com.aivideo.common.exception.BadRequestException;
import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.product.dto.ProductFields;
import com.aivideo.product.dto.ProductRequest;
import com.aivideo.product.dto.ProductResponse;
import com.aivideo.project.Project;
import com.aivideo.project.ProjectRepository;
import com.aivideo.project.ProjectStatus;
import com.aivideo.project.WorkflowType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProjectRepository projectRepository;
    @Mock
    ProductRepository productRepository;

    ProductService productService;

    UUID projectId;
    Project project;

    @BeforeEach
    void setUp() {
        productService = new ProductService(projectRepository, productRepository,
                List.of(new ManualProductProvider()));
        projectId = UUID.randomUUID();
        project = Project.builder()
                .name("P").workflowType(WorkflowType.TIKTOK_PRODUCT)
                .status(ProjectStatus.CREATED).build();
        project.setId(projectId);
    }

    private void mockProject() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
    }

    private ProductRequest manualRequest() {
        return new ProductRequest("http://shop/p/1", new ProductFields("Bo ngu", "Fashion",
                "Mem", List.of("mem"), "thun", "den", "M", "nu",
                List.of("re"), "199000"));
    }

    private void mockSave() {
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.randomUUID());
            }
            return p;
        });
    }

    @Test
    void upsertCreatesProductFromManualData() {
        mockProject();
        when(productRepository.findByProjectId(projectId)).thenReturn(Optional.empty());
        mockSave();

        ProductResponse response = productService.upsertProduct(projectId, manualRequest());

        assertThat(response.name()).isEqualTo("Bo ngu");
        assertThat(response.productUrl()).isEqualTo("http://shop/p/1");
        assertThat(response.projectId()).isEqualTo(projectId);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void upsertUpdatesExistingProduct() {
        mockProject();
        Product existing = new Product();
        existing.setId(UUID.randomUUID());
        existing.setProject(project);
        existing.setName("Old");
        when(productRepository.findByProjectId(projectId)).thenReturn(Optional.of(existing));
        mockSave();

        ProductResponse response = productService.upsertProduct(projectId, manualRequest());

        assertThat(response.id()).isEqualTo(existing.getId());
        assertThat(response.name()).isEqualTo("Bo ngu");
    }

    @Test
    void upsertUrlOnlyCreatesShellProduct() {
        mockProject();
        when(productRepository.findByProjectId(projectId)).thenReturn(Optional.empty());
        mockSave();

        ProductResponse response = productService.upsertProduct(projectId,
                new ProductRequest("http://shop/p/9", null));

        assertThat(response.productUrl()).isEqualTo("http://shop/p/9");
        assertThat(response.name()).isNull();
    }

    @Test
    void upsertEmptyInputThrows() {
        mockProject();
        assertThatThrownBy(() -> productService.upsertProduct(projectId,
                new ProductRequest(null, null)))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getCode())
                        .isEqualTo("PRODUCT_INPUT_REQUIRED"));
    }

    @Test
    void upsertMissingProjectThrowsNotFound() {
        UUID missing = UUID.randomUUID();
        when(projectRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.upsertProduct(missing, manualRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
