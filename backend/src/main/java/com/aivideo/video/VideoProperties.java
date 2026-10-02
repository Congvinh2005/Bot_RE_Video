package com.aivideo.video;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Chuẩn TikTok: 9:16 dọc, 1080x1920, H.264 + AAC, tối đa 30s.
 * Docker image (Task 18) override VIDEO_FONT_PATH sang font có trong image.
 */
@ConfigurationProperties(prefix = "app.video")
public record VideoProperties(
        String ffmpegPath,
        String fontPath,
        int width,
        int height,
        int fps,
        int maxDurationSeconds
) {
}
