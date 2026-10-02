package com.aivideo.job;

import com.aivideo.ai.AiException;
import com.aivideo.analysis.AnalysisService;
import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.content.ContentService;
import com.aivideo.content.dto.ContentResponse;
import com.aivideo.export.ExportTaskRepository;
import com.aivideo.project.Project;
import com.aivideo.project.ProjectRepository;
import com.aivideo.project.ProjectStatus;
import com.aivideo.project.WorkflowType;
import com.aivideo.video.GeneratedVideoRepository;
import com.aivideo.video.SubtitleService;
import com.aivideo.video.VideoService;
import com.aivideo.video.dto.SubtitleResponse;
import com.aivideo.video.dto.VideoResultResponse;
import com.aivideo.voice.VoiceService;
import com.aivideo.voice.dto.VoiceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobOrchestratorTest {

    @Mock
    ProjectRepository projectRepository;
    @Mock
    VideoJobRepository videoJobRepository;
    @Mock
    ExportTaskRepository exportTaskRepository;
    @Mock
    GeneratedVideoRepository generatedVideoRepository;
    @Mock
    AnalysisService analysisService;
    @Mock
    ContentService contentService;
    @Mock
    VoiceService voiceService;
    @Mock
    SubtitleService subtitleService;
    @Mock
    VideoService videoService;

    JobOrchestrator orchestrator;

    UUID projectId;
    Project project;

    @BeforeEach
    void setUp() {
        orchestrator = new JobOrchestrator(projectRepository, videoJobRepository,
                exportTaskRepository, generatedVideoRepository, analysisService,
                contentService, voiceService, subtitleService, videoService);
        projectId = UUID.randomUUID();
        project = Project.builder().name("P")
                .workflowType(WorkflowType.NORMAL_VIDEO).status(ProjectStatus.CREATED).build();
        project.setId(projectId);
    }

    private VideoJob savedJob() {
        VideoJob job = VideoJob.builder().project(project)
                .status(JobStatus.PENDING).progress(0).build();
        job.setId(UUID.randomUUID());
        when(videoJobRepository.save(any(VideoJob.class))).thenReturn(job);
        when(videoJobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        return job;
    }

    private void mockStepsSuccess() {
        UUID contentId = UUID.randomUUID();
        UUID voiceId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        when(contentService.generateContent(any(), any())).thenReturn(
                new ContentResponse(contentId, projectId, 1, "H", "S", "C",
                        List.of(), "CTA", List.of(), Instant.now()));
        when(voiceService.generateVoice(any(), any())).thenReturn(
                new VoiceResponse(UUID.randomUUID(), projectId, contentId, "ADAM",
                        1.0, 0.0, "natural", "vi", "mp3", "audio/x.mp3",
                        "http://cdn/x.mp3", "COMPLETED", Instant.now()));
        when(subtitleService.generateSubtitle(any(), any())).thenReturn(
                new SubtitleResponse("subtitle/x.srt", "http://cdn/x.srt", 3, 10.0));
        when(videoService.generateVideo(any(), any())).thenReturn(
                new VideoResultResponse(videoId, projectId, "video/f.mp4",
                        "http://cdn/f.mp4", "http://cdn/t.jpg", 25.0,
                        1080, 1920, "COMPLETED", Instant.now()));
        when(generatedVideoRepository.findById(videoId)).thenReturn(Optional.empty());
    }

    @Test
    void launchCreatesPendingJobAndMarksProjectProcessing() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        VideoJob job = VideoJob.builder().project(project)
                .status(JobStatus.PENDING).progress(0).build();
        job.setId(UUID.randomUUID());
        when(videoJobRepository.save(any(VideoJob.class))).thenReturn(job);

        VideoJob launched = orchestrator.launchFullPipeline(projectId, "ctx");

        assertThat(launched.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.PROCESSING);
    }

    @Test
    void runFullPipelineCompletesAllSteps() {
        VideoJob job = savedJob();
        mockStepsSuccess();
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        orchestrator.runFullPipeline(job.getId(), "ctx");

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(job.getProgress()).isEqualTo(100);
        assertThat(job.getCurrentStep()).isEqualTo(JobStep.EXPORT);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        verify(exportTaskRepository).save(any());
    }

    @Test
    void runFullPipelineFailsFastOnStepError() {
        VideoJob job = savedJob();
        when(contentService.generateContent(any(), any()))
                .thenThrow(new AiException("AI_PROVIDER_ERROR", "down"));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> orchestrator.runFullPipeline(job.getId(), null))
                .isInstanceOf(AiException.class);
        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getCurrentStep()).isEqualTo(JobStep.CONTENT);
        assertThat(job.getErrorMessage()).isNotBlank();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.FAILED);
    }

    @Test
    void runMissingJobThrows() {
        UUID missing = UUID.randomUUID();
        when(videoJobRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orchestrator.runFullPipeline(missing, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .satisfies(ex -> assertThat(((ResourceNotFoundException) ex).getCode())
                        .isEqualTo("JOB_NOT_FOUND"));
    }
}
