package com.aivideo.video.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GenerateVideoRequest(
        @NotNull(message = "contentGenerationId must not be null")
        UUID contentGenerationId,

        @NotNull(message = "voiceGenerationId must not be null")
        UUID voiceGenerationId
) {
}
