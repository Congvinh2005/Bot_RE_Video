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
import com.aivideo.source.VideoMetadata;
import com.aivideo.source.VideoMetadataService;
import com.aivideo.source.VideoSource;
import com.aivideo.source.VideoSourceRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pipeline: FFprobe -> Scene Detection -> Frame Extraction -> Transcript
 * -> AI Analysis -> lưu VideoAnalysis. Chạy background, tracking bằng VideoJob.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisService {

    private final ProjectRepository projectRepository;
    private final VideoSourceRepository videoSourceRepository;
    private final VideoAnalysisRepository videoAnalysisRepository;
    private final VideoJobRepository videoJobRepository;
    private final MediaStorage mediaStorage;
    private final VideoMetadataService metadataService;
    private final SceneDetector sceneDetector;
    private final FrameExtractor frameExtractor;
    private final SttProvider sttProvider;
    private final AiProvider aiProvider;
    private final PromptRenderer promptRenderer;
    private final ObjectMapper objectMapper;
    private final AnalysisProperties properties;

    @Transactional
    public VideoJob launchAnalysis(UUID projectId, UUID videoSourceId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));
        VideoSource source = resolveSource(projectId, videoSourceId);
        VideoJob job = videoJobRepository.save(VideoJob.builder()
                .project(project)
                .status(JobStatus.PENDING)
                .progress(0)
                .currentStep(JobStep.ANALYZE)
                .traceId(MDC.get("traceId"))
                .build());
        log.info("VIDEO_ANALYSIS_STARTED project={} job={} source={}",
                projectId, job.getId(), source.getId());
        return job;
    }

    @Transactional
    public VideoAnalysis doAnalyze(UUID jobId) {
        VideoJob job = videoJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("JOB_NOT_FOUND",
                        "Job not found: " + jobId));
        mark(job, JobStatus.PROCESSING, 10, null);
        Path temp = null;
        try {
            VideoSource source = videoSourceRepository
                    .findFirstByProjectIdOrderByCreatedAtDesc(job.getProject().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("SOURCE_NOT_FOUND",
                            "No video source for project"));
            temp = downloadSource(source);
            VideoMetadata meta = metadataService.probe(temp);
            List<SceneSegment> scenes = sceneDetector.detect(temp,
                    meta.duration() != null ? meta.duration() : 0);
            String transcript = sttProvider.transcribe(temp).orElse("");
            String visualDescription = describeFirstFrame(temp, scenes);

            Map<String, Object> context = buildContext(source, meta, scenes, transcript,
                    visualDescription);
            String prompt = promptRenderer.render(PromptType.VIDEO_ANALYSIS,
                    properties.promptVersion(),
                    Map.of("video_context", "(see VIDEO_CONTEXT_JSON below)",
                            "language", properties.language()));
            VideoAnalysisResult result = aiProvider.analyzeVideoContextStructured(
                    prompt, context, objectMapper, VideoAnalysisResult.class);

            Map<String, Object> resultMap = objectMapper.convertValue(result,
                    new TypeReference<Map<String, Object>>() {
                    });
            videoAnalysisRepository.save(VideoAnalysis.builder()
                    .project(job.getProject())
                    .videoSource(source)
                    .duration(meta.duration())
                    .result(resultMap)
                    .build());
            mark(job, JobStatus.COMPLETED, 100, null);
            log.info("JOB_COMPLETED job={} step=ANALYZE", job.getId());
            return videoAnalysisRepository
                    .findByProjectIdOrderByCreatedAtDesc(job.getProject().getId())
                    .stream().findFirst().orElseThrow();
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            mark(job, JobStatus.FAILED, job.getProgress(), message);
            log.warn("JOB_FAILED job={} step=ANALYZE error={}", job.getId(), message);
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new AiException("VIDEO_ANALYSIS_FAILED", message, e);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private VideoSource resolveSource(UUID projectId, UUID videoSourceId) {
        if (videoSourceId != null) {
            return videoSourceRepository.findById(videoSourceId)
                    .filter(s -> s.getProject().getId().equals(projectId))
                    .orElseThrow(() -> new ResourceNotFoundException("SOURCE_NOT_FOUND",
                            "Video source not found: " + videoSourceId));
        }
        return videoSourceRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("SOURCE_NOT_FOUND",
                        "No video source for project: " + projectId));
    }

    private Path downloadSource(VideoSource source) throws Exception {
        Path temp = Files.createTempFile("analysis-", ".mp4");
        try (InputStream in = mediaStorage.download(source.getStorageKey());
             OutputStream out = Files.newOutputStream(temp)) {
            in.transferTo(out);
        }
        return temp;
    }

    private String describeFirstFrame(Path video, List<SceneSegment> scenes) {
        if (scenes.isEmpty()) {
            return "";
        }
        try {
            int capped = Math.min(scenes.size(), properties.maxFrames());
            SceneSegment first = scenes.subList(0, capped).get(0);
            double middle = (first.start() + first.end()) / 2;
            byte[] frame = frameExtractor.extractFrame(video, middle);
            return aiProvider.analyzeImage(
                    "Describe this video frame briefly: objects, setting, on-screen text.",
                    frame, "image/jpeg");
        } catch (Exception e) {
            log.warn("FRAME_DESCRIBE_FAILED error={}", e.getMessage());
            return "";
        }
    }

    private Map<String, Object> buildContext(VideoSource source, VideoMetadata meta,
                                             List<SceneSegment> scenes, String transcript,
                                             String visualDescription) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("sourceFilename", source.getOriginalFilename());
        context.put("duration", meta.duration());
        context.put("width", meta.width());
        context.put("height", meta.height());
        context.put("fps", meta.fps());
        context.put("hasAudio", meta.hasAudio());
        List<Map<String, Object>> sceneMaps = new ArrayList<>();
        for (SceneSegment scene : scenes) {
            sceneMaps.add(Map.of("start", scene.start(), "end", scene.end()));
        }
        context.put("scenes", sceneMaps);
        context.put("transcript", transcript);
        context.put("visualDescription", visualDescription);
        return context;
    }

    private void mark(VideoJob job, JobStatus status, int progress, String error) {
        job.setStatus(status);
        job.setProgress(progress);
        job.setErrorMessage(error);
        if (status == JobStatus.PROCESSING && job.getStartedAt() == null) {
            job.setStartedAt(java.time.Instant.now());
        }
        if (status == JobStatus.COMPLETED || status == JobStatus.FAILED) {
            job.setCompletedAt(java.time.Instant.now());
        }
        videoJobRepository.save(job);
    }
}
