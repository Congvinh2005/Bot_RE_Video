package com.aivideo.content;

import java.util.List;

/** Schema structured output của bước Content Generation (khớp prompt CONTENT_GENERATION). */
public record ContentResult(
        String hook,
        String script,
        String caption,
        List<String> hashtags,
        String cta,
        List<ContentScene> scenes
) {
    public record ContentScene(
            double start,
            double end,
            String voiceText,
            String overlayText,
            String purpose
    ) {
    }
}
