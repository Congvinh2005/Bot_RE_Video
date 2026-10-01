package com.aivideo.content;

import com.aivideo.analysis.VideoAnalysis;
import com.aivideo.analysis.VideoAnalysisRepository;
import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.product.Product;
import com.aivideo.product.ProductRepository;
import com.aivideo.project.Project;
import com.aivideo.project.ProjectRepository;
import com.aivideo.project.ProjectStatus;
import com.aivideo.project.WorkflowType;
import com.aivideo.source.SourceStatus;
import com.aivideo.source.SourceType;
import com.aivideo.source.VideoSource;
import com.aivideo.source.VideoSourceRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContextEngineTest {

    @Mock
    ProjectRepository projectRepository;
    @Mock
    VideoAnalysisRepository videoAnalysisRepository;
    @Mock
    ProductRepository productRepository;
    @Mock
    VideoSourceRepository videoSourceRepository;

    ContextEngine engine;

    UUID projectId;
    Project project;

    @BeforeEach
    void setUp() {
        engine = new ContextEngine(projectRepository, videoAnalysisRepository,
                productRepository, videoSourceRepository);
        projectId = UUID.randomUUID();
        project = Project.builder().name("P")
                .workflowType(WorkflowType.NORMAL_VIDEO).status(ProjectStatus.CREATED).build();
        project.setId(projectId);
    }

    private void mockProject() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
    }

    private void mockFullData() {
        mockProject();
        VideoAnalysis analysis = VideoAnalysis.builder().project(project)
                .duration(10.0).result(Map.of("hook", "H", "tone", "fun")).build();
        when(videoAnalysisRepository.findByProjectIdOrderByCreatedAtDesc(projectId))
                .thenReturn(List.of(analysis));

        Product product = Product.builder().project(project).name("Bo ngu")
                .price("199000").color("den").build();
        when(productRepository.findByProjectId(projectId)).thenReturn(Optional.of(product));

        VideoSource source = VideoSource.builder().project(project)
                .sourceType(SourceType.UPLOAD).originalFilename("a.mp4")
                .duration(10.0).status(SourceStatus.READY).build();
        when(videoSourceRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId))
                .thenReturn(Optional.of(source));
    }

    @Test
    void buildMergesAllContexts() {
        mockFullData();

        UnifiedContext context = engine.buildUnifiedContext(projectId, "ban dem, content ngan");

        assertThat(context.video()).containsEntry("hook", "H");
        assertThat(context.product()).containsEntry("name", "Bo ngu");
        assertThat(context.product()).containsEntry("price", "199000");
        assertThat(context.product()).doesNotContainKey("category");
        assertThat(context.source()).containsEntry("sourceType", "UPLOAD");
        assertThat(context.source()).containsEntry("originalFilename", "a.mp4");
        assertThat(context.userContext()).containsEntry("text", "ban dem, content ngan");
    }

    @Test
    void buildToleratesMissingPieces() {
        mockProject();
        when(videoAnalysisRepository.findByProjectIdOrderByCreatedAtDesc(projectId))
                .thenReturn(List.of());
        when(productRepository.findByProjectId(projectId)).thenReturn(Optional.empty());
        when(videoSourceRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId))
                .thenReturn(Optional.empty());

        UnifiedContext context = engine.buildUnifiedContext(projectId, null);

        assertThat(context.video()).isEmpty();
        assertThat(context.product()).isEmpty();
        assertThat(context.source()).isEmpty();
        assertThat(context.userContext()).isEmpty();
    }

    @Test
    void buildBlankUserContextStaysEmpty() {
        mockFullData();

        UnifiedContext context = engine.buildUnifiedContext(projectId, "   ");

        assertThat(context.userContext()).isEmpty();
    }

    @Test
    void buildMissingProjectThrows() {
        UUID missing = UUID.randomUUID();
        when(projectRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> engine.buildUnifiedContext(missing, "x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void buildUsesLatestAnalysis() {
        mockProject();
        VideoAnalysis old = VideoAnalysis.builder().project(project)
                .result(Map.of("hook", "OLD")).build();
        VideoAnalysis latest = VideoAnalysis.builder().project(project)
                .result(Map.of("hook", "NEW")).build();
        when(videoAnalysisRepository.findByProjectIdOrderByCreatedAtDesc(projectId))
                .thenReturn(List.of(latest, old));
        when(productRepository.findByProjectId(projectId)).thenReturn(Optional.empty());
        when(videoSourceRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId))
                .thenReturn(Optional.empty());

        UnifiedContext context = engine.buildUnifiedContext(projectId, null);

        assertThat(context.video()).containsEntry("hook", "NEW");
    }
}
