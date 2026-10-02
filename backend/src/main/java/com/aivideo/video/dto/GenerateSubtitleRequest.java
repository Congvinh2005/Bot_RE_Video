package com.aivideo.video.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GenerateSubtitleRequest(
        @NotNull(message = "voiceGenerationId must not be null")
        UUID voiceGenerationId
) {
}
