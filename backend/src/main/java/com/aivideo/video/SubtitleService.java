package com.aivideo.video;

import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.media.AssetType;
import com.aivideo.media.MediaAsset;
import com.aivideo.media.MediaAssetRepository;
import com.aivideo.media.MediaStorage;
import com.aivideo.media.StoragePath;
import com.aivideo.project.Project;
import com.aivideo.project.ProjectRepository;
import com.aivideo.source.VideoMetadata;
import com.aivideo.source.VideoMetadataService;
import com.aivideo.video.dto.SubtitleResponse;
import com.aivideo.voice.VoiceGeneration;
import com.aivideo.voice.VoiceGenerationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Sinh subtitle SRT đồng bộ với voice: đo duration audio thật bằng FFprobe,
 * chia cue tỉ lệ ký tự, lưu file vào object storage + ghi MediaAsset.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubtitleService {

    private final ProjectRepository projectRepository;
    private final VoiceGenerationRepository voiceGenerationRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final MediaStorage mediaStorage;
    private final VideoMetadataService metadataService;
    private final SubtitleGenerator subtitleGenerator;

    @Transactional
    public SubtitleResponse generateSubtitle(UUID projectId, UUID voiceGenerationId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));
        VoiceGeneration voice = voiceGenerationRepository.findById(voiceGenerationId)
                .filter(v -> v.getProject().getId().equals(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("VOICE_NOT_FOUND",
                        "Voice generation not found: " + voiceGenerationId));
        String script = voice.getContentGeneration() != null
                ? voice.getContentGeneration().getScript() : null;
        if (script == null || script.isBlank()) {
            throw new SubtitleException("SUBTITLE_FAILED", "Voice script is empty");
        }

        double duration = probeAudioDuration(voice.getStorageKey());
        String srt = subtitleGenerator.generate(script, duration);
        byte[] bytes = srt.getBytes(StandardCharsets.UTF_8);

        String key = StoragePath.pathFor(AssetType.SUBTITLE, "subtitle.srt");
        mediaStorage.upload(key, new ByteArrayInputStream(bytes), bytes.length,
                "application/x-subrip");
        mediaAssetRepository.save(MediaAsset.builder()
                .project(project)
                .assetType(AssetType.SUBTITLE)
                .storageKey(key)
                .filename("subtitle.srt")
                .mimeType("application/x-subrip")
                .sizeBytes((long) bytes.length)
                .build());

        int cueCount = countCues(srt);
        log.info("SUBTITLE_OK project={} cues={} duration={}", projectId, cueCount, duration);
        return new SubtitleResponse(key, mediaStorage.getUrl(key), cueCount, duration);
    }

    private double probeAudioDuration(String storageKey) {
        Path temp = null;
        try {
            temp = Files.createTempFile("subtitle-", ".mp3");
            try (InputStream in = mediaStorage.download(storageKey);
                 OutputStream out = Files.newOutputStream(temp)) {
                in.transferTo(out);
            }
            VideoMetadata meta = metadataService.probe(temp);
            if (meta.duration() == null || meta.duration() <= 0) {
                throw new SubtitleException("SUBTITLE_FAILED",
                        "Unable to determine audio duration");
            }
            return meta.duration();
        } catch (SubtitleException e) {
            throw e;
        } catch (Exception e) {
            throw new SubtitleException("SUBTITLE_FAILED", "Subtitle generation failed", e);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private int countCues(String srt) {
        int count = 0;
        for (String block : srt.split("\\n\\n")) {
            if (!block.isBlank()) {
                count++;
            }
        }
        return count;
    }
}
