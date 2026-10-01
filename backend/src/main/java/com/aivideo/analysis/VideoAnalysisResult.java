package com.aivideo.analysis;

import java.util.List;

/** Schema structured output của bước Video Analysis (khớp prompt VIDEO_ANALYSIS). */
public record VideoAnalysisResult(
        Double duration,
        List<SceneResult> scenes,
        List<String> detectedText,
        String transcript,
        String productDescription,
        String hook,
        String cta,
        String tone,
        String visualStyle
) {
    public record SceneResult(
            double start,
            double end,
            String description,
            String purpose
    ) {
    }
}
