package com.aivideo.voice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

/**
 * TTS qua ElevenLabs API cho voice ADAM (premade voice chính chủ).
 * speed/pitch/emotion được lưu DB để theo dõi; ElevenLabs hiện chỉ nhận
 * stability/similarity nên các option đó chưa map 1-1 vào request.
 */
@Component
@Slf4j
public class ElevenLabsVoiceProvider implements VoiceProvider {

    private final ElevenLabsProperties properties;
    private final RestClient restClient;

    public ElevenLabsVoiceProvider(ElevenLabsProperties properties) {
        this(properties, buildClient(properties));
    }

    ElevenLabsVoiceProvider(ElevenLabsProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    private static RestClient buildClient(ElevenLabsProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    public String voiceName() {
        return "ADAM";
    }

    @Override
    public byte[] generateSpeech(String text, VoiceOptions options) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new VoiceException("TTS_PROVIDER_ERROR",
                    "ElevenLabs API key is not configured (env ELEVENLABS_API_KEY)");
        }
        if (text == null || text.isBlank()) {
            throw new VoiceException("TTS_PROVIDER_ERROR", "Text must not be blank");
        }
        try {
            byte[] audio = restClient.post()
                    .uri("/v1/text-to-speech/{voiceId}?output_format={format}",
                            properties.voiceId(), properties.outputFormat())
                    .header("xi-api-key", properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "text", text,
                            "model_id", properties.model(),
                            "language_code", properties.languageCode(),
                            "voice_settings", Map.of(
                                    "stability", 0.5,
                                    "similarity_boost", 0.75)))
                    .retrieve()
                    .body(byte[].class);
            if (audio == null || audio.length == 0) {
                throw new VoiceException("TTS_PROVIDER_ERROR", "TTS returned empty audio");
            }
            log.info("TTS_OK voice=ADAM bytes={}", audio.length);
            return audio;
        } catch (VoiceException e) {
            throw e;
        } catch (Exception e) {
            throw new VoiceException("TTS_PROVIDER_ERROR", "TTS provider call failed", e);
        }
    }
}
