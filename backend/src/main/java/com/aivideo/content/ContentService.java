package com.aivideo.content;

import com.aivideo.ai.AiProvider;
import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.content.dto.ContentResponse;
import com.aivideo.project.Project;
import com.aivideo.project.ProjectRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI Content Generator chỉ nhận UnifiedContext (từ ContextEngine),
 * render prompt CONTENT_GENERATION và lưu từng version để regenerate.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentService {

    private final ProjectRepository projectRepository;
    private final ContentGenerationRepository contentGenerationRepository;
    private final ContextEngine contextEngine;
    private final AiProvider aiProvider;
    private final PromptRenderer promptRenderer;
    private final ObjectMapper objectMapper;
    private final ContentProperties properties;

    @Transactional
    public ContentResponse generateContent(UUID projectId, String userContext) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));
        UnifiedContext unified = contextEngine.buildUnifiedContext(projectId, userContext);
        String prompt;
        try {
            String unifiedJson = objectMapper.writeValueAsString(Map.of(
                    "source", unified.source(),
                    "product", unified.product(),
                    "video", unified.video(),
                    "userContext", unified.userContext()));
            prompt = promptRenderer.render(PromptType.CONTENT_GENERATION,
                    properties.promptVersion(),
                    Map.of("unified_context", unifiedJson,
                            "language", properties.language()));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to build content prompt", e);
        }

        ContentResult result = aiProvider.generateStructured(prompt, ContentResult.class);
        int version = contentGenerationRepository.findByProjectIdOrderByVersionDesc(projectId)
                .stream().mapToInt(ContentGeneration::getVersion).max().orElse(0) + 1;

        ContentGeneration saved = contentGenerationRepository.save(ContentGeneration.builder()
                .project(project)
                .version(version)
                .hook(result.hook())
                .script(result.script())
                .caption(result.caption())
                .hashtags(result.hashtags())
                .cta(result.cta())
                .scenes(toMaps(result.scenes()))
                .result(toMap(result))
                .build());
        log.info("AI_CONTENT_OK project={} version={}", projectId, version);
        return ContentResponse.from(saved);
    }

    private List<Map<String, Object>> toMaps(Object value) {
        if (value == null) {
            return List.of();
        }
        return objectMapper.convertValue(value, new TypeReference<List<Map<String, Object>>>() {
        });
    }

    private Map<String, Object> toMap(Object value) {
        if (value == null) {
            return Map.of();
        }
        return objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() {
        });
    }
}
