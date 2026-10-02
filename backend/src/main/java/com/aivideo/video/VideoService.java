package com.aivideo.video;

import com.aivideo.analysis.FrameExtractor;
import com.aivideo.common.exception.BadRequestException;
import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.content.ContentGeneration;
import com.aivideo.content.ContentGenerationRepository;
import com.aivideo.media.AssetType;
import com.aivideo.media.MediaAsset;
import com.aivideo.media.MediaAssetRepository;
import com.aivideo.media.MediaStorage;
import com.aivideo.media.StoragePath;
import com.aivideo.project.Project;
import com.aivideo.project.ProjectRepository;
import com.aivideo.source.VideoMetadata;
import com.aivideo.source.VideoMetadataService;
import com.aivideo.source.VideoSource;
import com.aivideo.source.VideoSourceRepository;
import com.aivideo.video.dto.GenerateVideoRequest;
import com.aivideo.video.dto.VideoResultResponse;
import com.aivideo.voice.VoiceGeneration;
import com.aivideo.voice.VoiceGenerationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Dựng video hoàn chỉnh: source + voice ADAM + subtitle + overlay theo scene plan.
 * Output luôn 1080x1920 H.264/AAC, tối đa 30s (chuẩn TikTok 9:16).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final ProjectRepository projectRepository;
    private final ContentGenerationRepository contentGenerationRepository;
    private final VoiceGenerationRepository voiceGenerationRepository;
    private final VideoSourceRepository videoSourceRepository;
    private final GeneratedVideoRepository generatedVideoRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final MediaStorage mediaStorage;
    private final VideoMetadataService metadataService;
    private final FrameExtractor frameExtractor;
    private final SubtitleGenerator subtitleGenerator;
    private final VideoComposer videoComposer;

    @Transactional
    public VideoResultResponse generateVideo(UUID projectId, GenerateVideoRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));
        ContentGeneration content = contentGenerationRepository.findById(request.contentGenerationId())
                .filter(c -> c.getProject().getId().equals(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("CONTENT_NOT_FOUND",
                        "Content generation not found"));
        VoiceGeneration voice = voiceGenerationRepository.findById(request.voiceGenerationId())
                .filter(v -> v.getProject().getId().equals(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("VOICE_NOT_FOUND",
                        "Voice generation not found"));
        if (!"COMPLETED".equals(voice.getStatus()) || voice.getStorageKey() == null) {
            throw new BadRequestException("VOICE_NOT_READY",
                    "Voice is not ready yet, generate voice first");
        }
        VideoSource source = latestUsableSource(projectId);
        if (content.getScript() == null || content.getScript().isBlank()) {
            throw new BadRequestException("CONTENT_EMPTY", "Content script is empty");
        }

        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("compose-");
            Path sourceFile = downloadTo(source.getStorageKey(), workDir.resolve("source.mp4"));
            Path voiceFile = downloadTo(voice.getStorageKey(), workDir.resolve("voice.mp3"));

            VideoMetadata voiceMeta = metadataService.probe(voiceFile);
            if (voiceMeta.duration() == null || voiceMeta.duration() <= 0) {
                throw new BadRequestException("VOICE_INVALID", "Unable to read voice duration");
            }
            Path subtitleFile = workDir.resolve("subs.srt");
            Files.write(subtitleFile,
                    subtitleGenerator.generate(content.getScript(), voiceMeta.duration())
                            .getBytes(StandardCharsets.UTF_8));

            List<VideoComposer.Overlay> overlays = toOverlays(content.getScenes());
            Path output = videoComposer.compose(sourceFile, voiceFile, subtitleFile,
                    overlays, voiceMeta.duration());
            VideoMetadata outMeta = metadataService.probe(output);

            String videoKey = StoragePath.pathFor(AssetType.VIDEO, "final.mp4");
            uploadFile(output, videoKey, "video/mp4");

            byte[] thumbnail = frameExtractor.extractFrame(output,
                    Math.min(1.0, outMeta.duration() / 2));
            String thumbKey = StoragePath.pathFor(AssetType.THUMBNAIL, "thumb.jpg");
            mediaStorage.upload(thumbKey, new java.io.ByteArrayInputStream(thumbnail),
                    thumbnail.length, "image/jpeg");

            GeneratedVideo saved = generatedVideoRepository.save(GeneratedVideo.builder()
                    .project(project)
                    .storageKey(videoKey)
                    .thumbnailKey(thumbKey)
                    .duration(outMeta.duration())
                    .width(1080)
                    .height(1920)
                    .status("COMPLETED")
                    .build());
            saveAsset(project, AssetType.VIDEO, videoKey, "final.mp4", "video/mp4", output);
            saveAsset(project, AssetType.THUMBNAIL, thumbKey, "thumb.jpg",
                    "image/jpeg", null);
            log.info("VIDEO_GENERATION_OK project={} key={} duration={}",
                    projectId, videoKey, outMeta.duration());
            return VideoResultResponse.from(saved,
                    mediaStorage.getUrl(videoKey), mediaStorage.getUrl(thumbKey));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Video generation failed", e);
        } finally {
            deleteRecursive(workDir);
        }
    }

    private VideoSource latestUsableSource(UUID projectId) {
        return videoSourceRepository.findByProjectId(projectId).stream()
                .filter(s -> s.getStorageKey() != null && !s.getStorageKey().isBlank())
                .max(Comparator.comparing(VideoSource::getCreatedAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElseThrow(() -> new ResourceNotFoundException("SOURCE_NOT_FOUND",
                        "No uploaded source video, upload a video first"));
    }

    private Path downloadTo(String storageKey, Path target) throws Exception {
        try (InputStream in = mediaStorage.download(storageKey);
             OutputStream out = Files.newOutputStream(target)) {
            in.transferTo(out);
        }
        return target;
    }

    private void uploadFile(Path file, String key, String mimeType) throws Exception {
        long size = Files.size(file);
        try (InputStream in = Files.newInputStream(file)) {
            mediaStorage.upload(key, in, size, mimeType);
        }
    }

    private void saveAsset(Project project, AssetType type, String key,
                           String filename, String mimeType, Path file) {
        Long size = null;
        if (file != null) {
            try {
                size = Files.size(file);
            } catch (Exception ignored) {
            }
        }
        mediaAssetRepository.save(MediaAsset.builder()
                .project(project)
                .assetType(type)
                .storageKey(key)
                .filename(filename)
                .mimeType(mimeType)
                .sizeBytes(size)
                .build());
    }

    private List<VideoComposer.Overlay> toOverlays(List<Map<String, Object>> scenes) {
        List<VideoComposer.Overlay> overlays = new ArrayList<>();
        if (scenes == null) {
            return overlays;
        }
        for (Map<String, Object> scene : scenes) {
            Object text = scene.get("overlayText");
            if (text instanceof String str && !str.isBlank()) {
                overlays.add(new VideoComposer.Overlay(str,
                        toDouble(scene.get("start")), toDouble(scene.get("end"))));
            }
        }
        return overlays;
    }

    private double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0;
    }

    private void deleteRecursive(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception ignored) {
        }
    }
}
