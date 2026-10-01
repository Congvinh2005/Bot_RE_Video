package com.aivideo.source.dto;

import com.aivideo.source.SourceStatus;
import com.aivideo.source.SourceType;
import com.aivideo.source.VideoSource;

import java.time.Instant;
import java.util.UUID;

public record VideoSourceResponse(
        UUID id,
        UUID projectId,
        SourceType sourceType,
        String originalFilename,
        String storageKey,
        Double duration,
        Integer width,
        Integer height,
        Double fps,
        String codec,
        Boolean hasAudio,
        String format,
        SourceStatus status,
        Instant createdAt
) {
    public static VideoSourceResponse from(VideoSource source) {
        return new VideoSourceResponse(
                source.getId(),
                source.getProject().getId(),
                source.getSourceType(),
                source.getOriginalFilename(),
                source.getStorageKey(),
                source.getDuration(),
                source.getWidth(),
                source.getHeight(),
                source.getFps(),
                source.getCodec(),
                source.getHasAudio(),
                source.getFormat(),
                source.getStatus(),
                source.getCreatedAt());
    }
}
