package com.aivideo.source.dto;

import jakarta.validation.constraints.NotBlank;

public record TikTokRequest(
        @NotBlank(message = "url must not be blank")
        String url,

        String context
) {
}
