package com.aivideo.content;

import java.util.Map;

/**
 * Context duy nhất mà AI Content Generator được phép nhận.
 * Mọi service khác không tự ráp prompt riêng.
 */
public record UnifiedContext(
        Map<String, Object> source,
        Map<String, Object> product,
        Map<String, Object> video,
        Map<String, Object> userContext
) {
}
