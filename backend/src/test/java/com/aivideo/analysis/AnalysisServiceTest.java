package com.aivideo.analysis;

import com.aivideo.ai.AiException;
import com.aivideo.ai.AiProvider;
import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.content.PromptRenderer;
import com.aivideo.content.PromptType;
import com.aivideo.job.JobStatus;
import com.aivideo.job.JobStep;
import com.aivideo.job.VideoJob;
import com.aivideo.job.VideoJobRepository;
import com.aivideo.media.MediaStorage;
import com.aivideo.project.Project;
import com.aivideo.project.ProjectRepository;
import com.aivideo.project.ProjectStatus;
import com.aivideo.project.WorkflowType;
import com.aivideo.source.VideoMetadata;
import com.aivideo.source.VideoMetadataService;
import com.aivideo.source.VideoSource;
import com.aivideo.source.VideoSourceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    ProjectRepository projectRepository;
    @Mock
    VideoSourceRepository videoSourceRepository;
    @Mock
    VideoAnalysisRepository videoAnalysisRepository;
    @Mock
    VideoJobRepository videoJobRepository;
    @Mock
    MediaStorage mediaStorage;
    @Mock
    VideoMetadataService metadataService;
    @Mock
    SceneDetector sceneDetector;
    @Mock
    FrameExtractor frameExtractor;
    @Mock
    SttProvider sttProvider;
    @Mock
    AiProvider aiProvider;
    @Mock
    PromptRenderer promptRenderer;

    AnalysisService service;

    UUID projectId;
    UUID sourceId;
    Project project;
    VideoSource source;

    @BeforeEach
    void setUp() {
        service = new AnalysisService(projectRepository, videoSourceRepository,
                videoAnalysisRepository, videoJobRepository, mediaStorage, metadataService,
                sceneDetector, frameExtractor, sttProvider, aiProvider, promptRenderer,
                new ObjectMapper(), new AnalysisProperties(0.4, 5, "ffmpeg", 1, "vi"));
        projectId = UUID.randomUUID();
        sourceId = UUID.randomUUID();
        project = Project.builder().name("P")
                .workflowType(WorkflowType.NORMAL_VIDEO).status(ProjectStatus.CREATED).build();
        project.setId(projectId);
        source = VideoSource.builder().project(project)
                .sourceType(com.aivideo.source.SourceType.UPLOAD)
                .originalFilename("a.mp4").storageKey("video/a.mp4")
                .status(com.aivideo.source.SourceStatus.READY).build();
        source.setId(sourceId);
    }

    private void mockJobSave() {
        when(videoJobRepository.save(any(VideoJob.class))).thenAnswer(inv -> {
            VideoJob job = inv.getArgument(0);
            if (job.getId() == null) {
                job.setId(UUID.randomUUID());
            }
            return job;
        });
    }

    private VideoJob runningJob() {
        VideoJob job = VideoJob.builder().project(project)
                .status(JobStatus.PROCESSING).progress(10)
                .currentStep(JobStep.ANALYZE).build();
        job.setId(UUID.randomUUID());
        when(videoJobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        return job;
    }

    private void mockPipelineSuccess() throws Exception {
        when(mediaStorage.download("video/a.mp4"))
                .thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        when(metadataService.probe(any())).thenReturn(
                new VideoMetadata(10.0, 640, 480, 30.0, "h264", "aac", true, "mp4"));
        when(sceneDetector.detect(any(), anyDouble()))
                .thenReturn(List.of(new SceneSegment(0, 5), new SceneSegment(5, 10)));
        when(sttProvider.transcribe(any())).thenReturn(Optional.of("hello shop"));
        when(frameExtractor.extractFrame(any(), anyDouble()))
                .thenReturn(new byte[]{(byte) 0xFF, (byte) 0xD8, 1});
        when(aiProvider.analyzeImage(anyString(), any(), anyString()))
                .thenReturn("a person holding clothes");
        when(promptRenderer.render(any(PromptType.class), any(Integer.class), any()))
                .thenReturn("prompt");
        when(aiProvider.analyzeVideoContextStructured(anyString(), any(), any(),
                eq(VideoAnalysisResult.class)))
                .thenReturn(new VideoAnalysisResult(10.0,
                        List.of(new VideoAnalysisResult.SceneResult(0, 5, "intro", "HOOK")),
                        List.of(), "hello shop", "clothes", "New!", "Buy", "fun", "bright"));
        when(videoAnalysisRepository.save(any(VideoAnalysis.class))).thenAnswer(inv -> {
            VideoAnalysis analysis = inv.getArgument(0);
            analysis.setId(UUID.randomUUID());
            return analysis;
        });
        when(videoAnalysisRepository.findByProjectIdOrderByCreatedAtDesc(projectId))
                .thenAnswer(inv -> List.of(videoAnalysisRepository.save(
                        VideoAnalysis.builder().project(project).result(Map.of()).build())));
    }

    @Test
    void launchCreatesPendingJobWithLatestSource() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(videoSourceRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId))
                .thenReturn(Optional.of(source));
        mockJobSave();

        VideoJob job = service.launchAnalysis(projectId, null);

        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(job.getCurrentStep()).isEqualTo(JobStep.ANALYZE);
    }

    @Test
    void launchWithExplicitSourceId() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(videoSourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        mockJobSave();

        VideoJob job = service.launchAnalysis(projectId, sourceId);

        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
    }

    @Test
    void launchMissingProjectThrows() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.launchAnalysis(projectId, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void doAnalyzeCompletesJobAndSavesAnalysis() throws Exception {
        VideoJob job = runningJob();
        when(videoSourceRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId))
                .thenReturn(Optional.of(source));
        mockPipelineSuccess();
        mockJobSave();

        service.doAnalyze(job.getId());

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(job.getProgress()).isEqualTo(100);
    }

    @Test
    void doAnalyzeMarksJobFailedOnAiError() throws Exception {
        VideoJob job = runningJob();
        when(videoSourceRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId))
                .thenReturn(Optional.of(source));
        when(mediaStorage.download("video/a.mp4"))
                .thenReturn(new ByteArrayInputStream(new byte[]{1}));
        when(metadataService.probe(any())).thenReturn(
                new VideoMetadata(10.0, 640, 480, 30.0, "h264", null, false, "mp4"));
        when(sceneDetector.detect(any(), anyDouble()))
                .thenReturn(List.of(new SceneSegment(0, 10)));
        when(sttProvider.transcribe(any())).thenReturn(Optional.empty());
        when(frameExtractor.extractFrame(any(), anyDouble()))
                .thenReturn(new byte[]{1});
        when(aiProvider.analyzeImage(anyString(), any(), anyString())).thenReturn("");
        when(promptRenderer.render(any(PromptType.class), any(Integer.class), any()))
                .thenReturn("prompt");
        when(aiProvider.analyzeVideoContextStructured(anyString(), any(), any(),
                eq(VideoAnalysisResult.class)))
                .thenThrow(new AiException("AI_PROVIDER_ERROR", "down"));
        mockJobSave();

        assertThatThrownBy(() -> service.doAnalyze(job.getId()))
                .isInstanceOf(AiException.class);
        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrorMessage()).isNotBlank();
    }
}
