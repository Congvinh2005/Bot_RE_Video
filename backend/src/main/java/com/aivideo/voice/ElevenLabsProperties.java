package com.aivideo.voice;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Key/voice/model lấy từ env. Voice mặc định là Adam (premade voice của
 * ElevenLabs). Tiếng Việt bắt buộc model eleven_v3 (multilingual_v2 không
 * hỗ trợ vi). Kiểm tra lại voice-id trong Voice Library nếu cần.
 */
@ConfigurationProperties(prefix = "app.tts.elevenlabs")
public record ElevenLabsProperties(
        String baseUrl,
        String apiKey,
        String voiceId,
        String model,
        String languageCode,
        String outputFormat,
        int timeoutSeconds
) {
}
