package com.aivideo.source;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Gọi TikTok oEmbed API công khai. oEmbed không trả về video file nên hệ thống
 * không bao giờ có asset trực tiếp từ TikTok — luôn yêu cầu user upload
 * video mà họ có quyền sử dụng.
 */
@Component
@Slf4j
public class TikTokOEmbedClientImpl implements TikTokOEmbedClient {

    private static final String OEMBED_ENDPOINT = "https://www.tiktok.com/oembed?url=";

    private final RestClient restClient = RestClient.create();

    @Override
    public Optional<OEmbedData> fetch(String videoUrl) {
        try {
            String target = OEMBED_ENDPOINT + URLEncoder.encode(videoUrl, StandardCharsets.UTF_8);
            OEmbedData data = restClient.get()
                    .uri(target)
                    .retrieve()
                    .body(OEmbedData.class);
            return Optional.ofNullable(data);
        } catch (Exception e) {
            log.warn("TIKTOK_OEMBED_FAILED url={} error={}", videoUrl, e.getMessage());
            return Optional.empty();
        }
    }
}
