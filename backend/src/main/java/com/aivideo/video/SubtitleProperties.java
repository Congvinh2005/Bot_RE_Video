package com.aivideo.video;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.subtitle")
public record SubtitleProperties(
        int maxCharsPerLine
) {
}
