package com.aivideo.product;

import com.aivideo.common.exception.BadRequestException;
import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.product.dto.ProductResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    ProductService productService;

    private ProductResponse sample(UUID projectId) {
        return new ProductResponse(UUID.randomUUID(), projectId, "Bo ngu", "Fashion",
                "Mem", List.of("mem"), "thun", "den", "M", "nu",
                List.of("re"), "199000", "http://shop/p/1", Instant.now());
    }

    @Test
    void upsertReturns200WithProduct() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(productService.upsertProduct(eq(projectId), any())).thenReturn(sample(projectId));

        mockMvc.perform(post("/api/projects/{id}/product", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productUrl", "http://shop/p/1",
                                "product", Map.of("name", "Bo ngu")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bo ngu"))
                .andExpect(jsonPath("$.productUrl").value("http://shop/p/1"));
    }

    @Test
    void upsertEmptyInputReturns400() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(productService.upsertProduct(eq(projectId), any())).thenThrow(
                new BadRequestException("PRODUCT_INPUT_REQUIRED", "empty"));

        mockMvc.perform(post("/api/projects/{id}/product", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PRODUCT_INPUT_REQUIRED"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void upsertMissingProjectReturns404() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(productService.upsertProduct(eq(projectId), any())).thenThrow(
                new ResourceNotFoundException("PROJECT_NOT_FOUND", "missing"));

        mockMvc.perform(post("/api/projects/{id}/product", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("productUrl", "http://shop/p/1"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }
}
