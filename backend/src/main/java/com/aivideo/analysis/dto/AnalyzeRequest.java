package com.aivideo.analysis.dto;

import java.util.UUID;

public record AnalyzeRequest(
        UUID videoSourceId
) {
}
