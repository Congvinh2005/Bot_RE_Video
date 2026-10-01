package com.aivideo.source;

/** Metadata video/source lấy qua API công khai được phép (không bypass, không scrape). */
public record SourceMetadata(
        String title,
        String author,
        String authorUrl,
        String thumbnailUrl
) {
}
