package com.aivideo.result;

import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.content.ContentGenerationRepository;
import com.aivideo.content.dto.ContentResponse;
import com.aivideo.media.MediaStorage;
import com.aivideo.product.ProductRepository;
import com.aivideo.product.dto.ProductResponse;
import com.aivideo.project.ProjectRepository;
import com.aivideo.video.GeneratedVideo;
import com.aivideo.video.GeneratedVideoRepository;
import com.aivideo.video.dto.VideoResultResponse;
import com.aivideo.voice.VoiceGeneration;
import com.aivideo.voice.VoiceGenerationRepository;
import com.aivideo.voice.dto.VoiceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResultService {

    private final ProjectRepository projectRepository;
    private final ContentGenerationRepository contentGenerationRepository;
    private final VoiceGenerationRepository voiceGenerationRepository;
    private final GeneratedVideoRepository generatedVideoRepository;
    private final ProductRepository productRepository;
    private final MediaStorage mediaStorage;

    @Transactional(readOnly = true)
    public ProjectResultResponse getResult(UUID projectId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));

        ContentResponse content = contentGenerationRepository
                .findByProjectIdOrderByVersionDesc(projectId).stream()
                .findFirst().map(ContentResponse::from).orElse(null);

        VoiceResponse voice = voiceGenerationRepository.findByProjectId(projectId).stream()
                .max(Comparator.comparing(VoiceGeneration::getCreatedAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(v -> VoiceResponse.from(v, url(v.getStorageKey())))
                .orElse(null);

        VideoResultResponse video = generatedVideoRepository.findByProjectId(projectId).stream()
                .max(Comparator.comparing(GeneratedVideo::getCreatedAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(v -> VideoResultResponse.from(v, url(v.getStorageKey()),
                        v.getThumbnailKey() != null ? url(v.getThumbnailKey()) : null))
                .orElse(null);

        ProductResponse product = productRepository.findByProjectId(projectId)
                .map(ProductResponse::from).orElse(null);

        return new ProjectResultResponse(content, voice, video, product);
    }

    private String url(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return null;
        }
        try {
            return mediaStorage.getUrl(storageKey);
        } catch (Exception e) {
            return null;
        }
    }
}
