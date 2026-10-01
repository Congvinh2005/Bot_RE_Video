package com.aivideo.source;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Provider TikTok: chỉ resolve metadata được phép (title/author/thumbnail).
 * Không tải video, không bypass. Vì oEmbed không cung cấp video asset nên
 * kết quả luôn là USER_UPLOAD_REQUIRED kèm metadata (nếu lấy được).
 */
@Component
@RequiredArgsConstructor
public class TikTokSourceProvider implements SourceProvider {

    private final TikTokOEmbedClient oEmbedClient;

    @Override
    public boolean supports(String url) {
        return TikTokUrlValidator.isTikTokUrl(url);
    }

    @Override
    public SourceAnalysis analyze(String url, String userContext) {
        return oEmbedClient.fetch(url)
                .map(data -> new SourceAnalysis(
                        SourceStatus.USER_UPLOAD_REQUIRED,
                        new SourceMetadata(data.title(), data.authorName(),
                                data.authorUrl(), data.thumbnailUrl()),
                        "Found TikTok video metadata. Please upload the source video you have rights to use."))
                .orElseGet(() -> new SourceAnalysis(
                        SourceStatus.USER_UPLOAD_REQUIRED,
                        null,
                        "Could not access TikTok video directly. Please upload the source video you have rights to use."));
    }
}
