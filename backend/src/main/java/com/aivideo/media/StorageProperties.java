package com.aivideo.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Mọi giá trị lấy từ environment variables, không hard-code credential. */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket,
        int urlExpiryHours
) {
}
