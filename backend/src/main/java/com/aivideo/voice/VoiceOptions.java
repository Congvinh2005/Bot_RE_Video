package com.aivideo.voice;

public record VoiceOptions(
        String voice,
        Double speed,
        Double pitch,
        String emotion,
        String language,
        String format
) {
    public static VoiceOptions defaults() {
        return new VoiceOptions("ADAM", 1.0, 0.0, "natural", "vi", "mp3");
    }
}
