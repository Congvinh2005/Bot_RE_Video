package com.aivideo.analysis;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Ngưỡng scene detect, số frame tối đa gửi AI, phiên bản prompt. */
@ConfigurationProperties(prefix = "app.analysis")
public record AnalysisProperties(
        double sceneThreshold,
        int maxFrames,
        String ffmpegPath,
        int promptVersion,
        String language
) {
}
