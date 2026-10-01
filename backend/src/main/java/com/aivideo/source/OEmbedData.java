package com.aivideo.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Dữ liệu trả về từ TikTok oEmbed API công khai (không cần auth). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OEmbedData(
        String title,
        @JsonProperty("author_name") String authorName,
        @JsonProperty("author_url") String authorUrl,
        @JsonProperty("thumbnail_url") String thumbnailUrl
) {
}
