package com.aivideo.video.dto;

import com.aivideo.video.GeneratedVideo;

import java.time.Instant;
import java.util.UUID;

public record VideoResultResponse(
        UUID id,
        UUID projectId,
        String storageKey,
        String videoUrl,
        String thumbnailUrl,
        Double duration,
        int width,
        int height,
        String status,
        Instant createdAt
) {
    public static VideoResultResponse from(GeneratedVideo video, String videoUrl,
                                           String thumbnailUrl) {
        return new VideoResultResponse(
                video.getId(),
                video.getProject().getId(),
                video.getStorageKey(),
                videoUrl,
                thumbnailUrl,
                video.getDuration(),
                video.getWidth(),
                video.getHeight(),
                video.getStatus(),
                video.getCreatedAt());
    }
}
