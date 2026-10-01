package com.aivideo.common;

public record ApiError(
        String code,
        String message,
        Object details,
        String traceId
) {
}
