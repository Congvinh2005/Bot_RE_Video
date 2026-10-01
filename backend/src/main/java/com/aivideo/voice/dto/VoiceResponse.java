package com.aivideo.voice.dto;

import com.aivideo.voice.VoiceGeneration;

import java.time.Instant;
import java.util.UUID;

public record VoiceResponse(
        UUID id,
        UUID projectId,
        UUID contentGenerationId,
        String voice,
        Double speed,
        Double pitch,
        String emotion,
        String language,
        String format,
        String storageKey,
        String audioUrl,
        String status,
        Instant createdAt
) {
    public static VoiceResponse from(VoiceGeneration generation, String audioUrl) {
        return new VoiceResponse(
                generation.getId(),
                generation.getProject().getId(),
                generation.getContentGeneration() != null
                        ? generation.getContentGeneration().getId() : null,
                generation.getVoice(),
                generation.getSpeed(),
                generation.getPitch(),
                generation.getEmotion(),
                generation.getLanguage(),
                generation.getFormat(),
                generation.getStorageKey(),
                audioUrl,
                generation.getStatus(),
                generation.getCreatedAt());
    }
}
