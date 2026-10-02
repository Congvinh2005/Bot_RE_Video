package com.aivideo.video.dto;

public record SubtitleResponse(
        String storageKey,
        String subtitleUrl,
        int cueCount,
        double audioDurationSeconds
) {
}
