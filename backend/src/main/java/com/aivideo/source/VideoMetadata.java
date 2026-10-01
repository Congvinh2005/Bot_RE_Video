package com.aivideo.source;

public record VideoMetadata(
        Double duration,
        Integer width,
        Integer height,
        Double fps,
        String videoCodec,
        String audioCodec,
        boolean hasAudio,
        String format
) {
}
