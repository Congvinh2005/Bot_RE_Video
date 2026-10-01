package com.aivideo.content;

import com.aivideo.ai.AiException;
import com.aivideo.ai.AiProvider;
import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.content.dto.ContentResponse;
import com.aivideo.project.Project;
import com.aivideo.project.ProjectRepository;
import com.aivideo.project.ProjectStatus;
import com.aivideo.project.WorkflowType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

    @Mock
    ProjectRepository projectRepository;
    @Mock
    ContentGenerationRepository contentGenerationRepository;
    @Mock
    ContextEngine contextEngine;
    @Mock
    AiProvider aiProvider;
    @Mock
    PromptRenderer promptRenderer;

    ContentService service;

    UUID projectId;
    Project project;

    @BeforeEach
    void setUp() {
        service = new ContentService(projectRepository, contentGenerationRepository,
                contextEngine, aiProvider, promptRenderer, new ObjectMapper(),
                new ContentProperties(1, "vi"));
        projectId = UUID.randomUUID();
        project = Project.builder().name("P")
                .workflowType(WorkflowType.NORMAL_VIDEO).status(ProjectStatus.CREATED).build();
        project.setId(projectId);
    }

    private void mockProjectAndContext() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(contextEngine.buildUnifiedContext(eq(projectId), any()))
                .thenReturn(new UnifiedContext(Map.of("sourceType", "UPLOAD"),
                        Map.of("name", "Bo ngu"), Map.of("hook", "H"), Map.of()));
        when(promptRenderer.render(eq(PromptType.CONTENT_GENERATION), anyInt(), any()))
                .thenReturn("prompt");
    }

    private ContentResult aiResult() {
        return new ContentResult("Hook!", "Script body", "Caption",
                List.of("#fashion"), "Mua ngay",
                List.of(new ContentResult.ContentScene(0, 3, "Voice", "Overlay", "HOOK")));
    }

    private void mockSave() {
        when(contentGenerationRepository.save(any(ContentGeneration.class)))
                .thenAnswer(inv -> {
                    ContentGeneration g = inv.getArgument(0);
                    g.setId(UUID.randomUUID());
                    return g;
                });
    }

    @Test
    void generateFirstVersionIsOne() {
        mockProjectAndContext();
        when(contentGenerationRepository.findByProjectIdOrderByVersionDesc(projectId))
                .thenReturn(List.of());
        when(aiProvider.generateStructured(anyString(), eq(ContentResult.class)))
                .thenReturn(aiResult());
        mockSave();

        ContentResponse response = service.generateContent(projectId, "ctx");

        assertThat(response.version()).isEqualTo(1);
        assertThat(response.hook()).isEqualTo("Hook!");
        assertThat(response.scenes()).hasSize(1);
        assertThat(response.scenes().get(0)).containsEntry("voiceText", "Voice");
    }

    @Test
    void regenerateIncrementsVersion() {
        mockProjectAndContext();
        ContentGeneration v2 = ContentGeneration.builder().project(project).version(2).build();
        when(contentGenerationRepository.findByProjectIdOrderByVersionDesc(projectId))
                .thenReturn(List.of(v2));
        when(aiProvider.generateStructured(anyString(), eq(ContentResult.class)))
                .thenReturn(aiResult());
        mockSave();

        ContentResponse response = service.generateContent(projectId, null);

        assertThat(response.version()).isEqualTo(3);
    }

    @Test
    void aiErrorPropagates() {
        mockProjectAndContext();
        when(aiProvider.generateStructured(anyString(), eq(ContentResult.class)))
                .thenThrow(new AiException("AI_PROVIDER_ERROR", "down"));

        assertThatThrownBy(() -> service.generateContent(projectId, null))
                .isInstanceOf(AiException.class);
    }

    @Test
    void missingProjectThrows() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateContent(projectId, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
