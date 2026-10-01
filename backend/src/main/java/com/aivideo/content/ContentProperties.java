package com.aivideo.content;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.content")
public record ContentProperties(
        int promptVersion,
        String language
) {
}
