package com.aivideo.source;

public record SourceAnalysis(
        SourceStatus status,
        SourceMetadata metadata,
        String message
) {
}
