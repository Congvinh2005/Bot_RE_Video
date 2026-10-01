package com.aivideo.content.dto;

import com.aivideo.content.ContentGeneration;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ContentResponse(
        UUID id,
        UUID projectId,
        int version,
        String hook,
        String script,
        String caption,
        List<String> hashtags,
        String cta,
        List<Map<String, Object>> scenes,
        Instant createdAt
) {
    public static ContentResponse from(ContentGeneration generation) {
        return new ContentResponse(
                generation.getId(),
                generation.getProject().getId(),
                generation.getVersion(),
                generation.getHook(),
                generation.getScript(),
                generation.getCaption(),
                generation.getHashtags(),
                generation.getCta(),
                generation.getScenes(),
                generation.getCreatedAt());
    }
}
