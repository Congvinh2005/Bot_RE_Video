package com.aivideo.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** API key/model lấy từ environment variables. Không default key. */
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        String baseUrl,
        String apiKey,
        String model,
        int timeoutSeconds
) {
}
