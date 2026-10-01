package com.aivideo.source.dto;

import com.aivideo.source.SourceMetadata;
import com.aivideo.source.SourceStatus;

public record TikTokResponse(
        SourceStatus status,
        String message,
        SourceMetadata metadata,
        VideoSourceResponse videoSource
) {
}
