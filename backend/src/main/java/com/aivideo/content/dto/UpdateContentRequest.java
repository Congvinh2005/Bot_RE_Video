package com.aivideo.content.dto;

import java.util.List;

/** Sửa content: field nào null thì giữ nguyên bản mới nhất, lưu version mới. */
public record UpdateContentRequest(
        String hook,
        String script,
        String caption,
        List<String> hashtags,
        String cta
) {
}
