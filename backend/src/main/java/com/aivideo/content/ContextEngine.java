package com.aivideo.content;

import com.aivideo.analysis.VideoAnalysis;
import com.aivideo.analysis.VideoAnalysisRepository;
import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.product.Product;
import com.aivideo.product.ProductRepository;
import com.aivideo.project.Project;
import com.aivideo.project.ProjectRepository;
import com.aivideo.source.VideoSource;
import com.aivideo.source.VideoSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Gộp VideoAnalysis + Product + Source metadata + user context thành UnifiedContext. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContextEngine {

    private final ProjectRepository projectRepository;
    private final VideoAnalysisRepository videoAnalysisRepository;
    private final ProductRepository productRepository;
    private final VideoSourceRepository videoSourceRepository;

    public UnifiedContext buildUnifiedContext(UUID projectId, String userContextText) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));

        Map<String, Object> video = videoAnalysisRepository
                .findByProjectIdOrderByCreatedAtDesc(project.getId())
                .stream().findFirst()
                .map(VideoAnalysis::getResult)
                .map(LinkedHashMap::new)
                .orElseGet(LinkedHashMap::new);
        if (video.isEmpty()) {
            log.warn("CONTEXT_NO_ANALYSIS project={}", projectId);
        }

        Map<String, Object> product = productRepository.findByProjectId(project.getId())
                .map(this::productMap)
                .orElseGet(LinkedHashMap::new);

        Map<String, Object> source = videoSourceRepository
                .findFirstByProjectIdOrderByCreatedAtDesc(project.getId())
                .map(this::sourceMap)
                .orElseGet(LinkedHashMap::new);

        Map<String, Object> userContext = new LinkedHashMap<>();
        if (userContextText != null && !userContextText.isBlank()) {
            userContext.put("text", userContextText.strip());
        }
        return new UnifiedContext(source, product, video, userContext);
    }

    private Map<String, Object> productMap(Product product) {
        Map<String, Object> map = new LinkedHashMap<>();
        putIfPresent(map, "name", product.getName());
        putIfPresent(map, "category", product.getCategory());
        putIfPresent(map, "description", product.getDescription());
        putIfPresent(map, "features", product.getFeatures());
        putIfPresent(map, "material", product.getMaterial());
        putIfPresent(map, "color", product.getColor());
        putIfPresent(map, "size", product.getSize());
        putIfPresent(map, "targetAudience", product.getTargetAudience());
        putIfPresent(map, "sellingPoints", product.getSellingPoints());
        putIfPresent(map, "price", product.getPrice());
        putIfPresent(map, "productUrl", product.getProductUrl());
        return map;
    }

    private Map<String, Object> sourceMap(VideoSource source) {
        Map<String, Object> map = new LinkedHashMap<>();
        putIfPresent(map, "sourceUrl", source.getSourceUrl());
        map.put("sourceType", source.getSourceType() != null
                ? source.getSourceType().name() : null);
        putIfPresent(map, "originalFilename", source.getOriginalFilename());
        putIfPresent(map, "duration", source.getDuration());
        putIfPresent(map, "width", source.getWidth());
        putIfPresent(map, "height", source.getHeight());
        putIfPresent(map, "status", source.getStatus() != null
                ? source.getStatus().name() : null);
        return map;
    }

    private void putIfPresent(Map<String, Object> map, String key, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String text && text.isBlank()) {
            return;
        }
        map.put(key, value);
    }
}
