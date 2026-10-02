package com.aivideo.job;

import com.aivideo.analysis.AnalysisService;
import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.content.ContentService;
import com.aivideo.content.dto.ContentResponse;
import com.aivideo.export.ExportTask;
import com.aivideo.export.ExportTaskRepository;
import com.aivideo.project.Project;
import com.aivideo.project.ProjectRepository;
import com.aivideo.project.ProjectStatus;
import com.aivideo.video.GeneratedVideoRepository;
import com.aivideo.video.VideoService;
import com.aivideo.video.SubtitleService;
import com.aivideo.video.dto.GenerateVideoRequest;
import com.aivideo.video.dto.SubtitleResponse;
import com.aivideo.video.dto.VideoResultResponse;
import com.aivideo.voice.VoiceService;
import com.aivideo.voice.dto.GenerateVoiceRequest;
import com.aivideo.voice.dto.VoiceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Chạy full pipeline trên 1 VideoJob duy nhất:
 * ANALYZE -> CONTENT -> VOICE -> SUBTITLE -> VIDEO -> EXPORT.
 * Mỗi bước tái dùng service đã có, job cập nhật step + progress liên tục.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JobOrchestrator {

    private final ProjectRepository projectRepository;
    private final VideoJobRepository videoJobRepository;
    private final ExportTaskRepository exportTaskRepository;
    private final GeneratedVideoRepository generatedVideoRepository;
    private final AnalysisService analysisService;
    private final ContentService contentService;
    private final VoiceService voiceService;
    private final SubtitleService subtitleService;
    private final VideoService videoService;

    @Transactional
    public VideoJob launchFullPipeline(UUID projectId, String userContext) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));
        project.setStatus(ProjectStatus.PROCESSING);
        projectRepository.save(project);
        VideoJob job = videoJobRepository.save(VideoJob.builder()
                .project(project)
                .status(JobStatus.PENDING)
                .progress(0)
                .traceId(MDC.get("traceId"))
                .build());
        log.info("FULL_PIPELINE_STARTED project={} job={}", projectId, job.getId());
        return job;
    }

    @Transactional
    public void runFullPipeline(UUID jobId, String userContext) {
        VideoJob job = videoJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("JOB_NOT_FOUND",
                        "Job not found: " + jobId));
        UUID projectId = job.getProject().getId();
        try {
            step(job, JobStep.ANALYZE, 5);
            analysisService.doAnalyze(jobId);

            step(job, JobStep.CONTENT, 25);
            ContentResponse content = contentService.generateContent(projectId, userContext);

            step(job, JobStep.VOICE, 45);
            VoiceResponse voice = voiceService.generateVoice(projectId,
                    new GenerateVoiceRequest(content.id(), "ADAM", 1.0, 0.0, "natural"));

            step(job, JobStep.SUBTITLE, 65);
            SubtitleResponse subtitle = subtitleService.generateSubtitle(projectId, voice.id());
            log.info("PIPELINE_SUBTITLE_OK job={} cues={}", jobId, subtitle.cueCount());

            step(job, JobStep.VIDEO, 80);
            VideoResultResponse video = videoService.generateVideo(projectId,
                    new GenerateVideoRequest(content.id(), voice.id()));

            step(job, JobStep.EXPORT, 95);
            exportTaskRepository.save(ExportTask.builder()
                    .project(job.getProject())
                    .generatedVideo(generatedVideoRepository.findById(video.id()).orElse(null))
                    .status("COMPLETED")
                    .progress(100)
                    .downloadUrl(video.videoUrl())
                    .completedAt(Instant.now())
                    .build());

            finish(job, JobStatus.COMPLETED, 100, null);
            setProjectStatus(job, ProjectStatus.COMPLETED);
            log.info("FULL_PIPELINE_DONE job={}", jobId);
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            finish(job, JobStatus.FAILED, job.getProgress(), message);
            setProjectStatus(job, ProjectStatus.FAILED);
            log.warn("FULL_PIPELINE_FAILED job={} step={} error={}",
                    jobId, job.getCurrentStep(), message);
            throw e instanceof RuntimeException re ? re : new IllegalStateException(message, e);
        }
    }

    private void step(VideoJob job, JobStep step, int progress) {
        job.setStatus(JobStatus.PROCESSING);
        job.setCurrentStep(step);
        job.setProgress(progress);
        if (job.getStartedAt() == null) {
            job.setStartedAt(Instant.now());
        }
        videoJobRepository.save(job);
        log.info("PIPELINE_STEP job={} step={} progress={}", job.getId(), step, progress);
    }

    private void finish(VideoJob job, JobStatus status, int progress, String error) {
        job.setStatus(status);
        job.setProgress(progress);
        job.setErrorMessage(error);
        job.setCompletedAt(Instant.now());
        videoJobRepository.save(job);
    }

    private void setProjectStatus(VideoJob job, ProjectStatus status) {
        Project project = job.getProject();
        project.setStatus(status);
        projectRepository.save(project);
    }
}
