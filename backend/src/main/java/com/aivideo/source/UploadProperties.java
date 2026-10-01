package com.aivideo.source;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Giới hạn và định dạng file cấu hình qua env (MAX_UPLOAD_MB, FFPROBE_PATH...). */
@ConfigurationProperties(prefix = "app.upload")
public record UploadProperties(
        long maxFileSizeMb,
        List<String> allowedExtensions,
        List<String> allowedMimeTypes,
        String ffprobePath
) {
    public long maxFileSizeBytes() {
        return maxFileSizeMb * 1024 * 1024;
    }
}
