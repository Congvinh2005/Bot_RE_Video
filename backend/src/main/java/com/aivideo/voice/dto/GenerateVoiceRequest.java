package com.aivideo.voice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GenerateVoiceRequest(
        @NotNull(message = "contentGenerationId must not be null")
        UUID contentGenerationId,

        String voice,
        Double speed,
        Double pitch,
        String emotion
) {
}
