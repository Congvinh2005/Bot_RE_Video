package com.aivideo.source;

/**
 * Abstraction nguồn video. Mỗi nền tảng (TikTok, ...) có một provider riêng.
 * Provider chỉ được dùng API/dữ liệu được phép, tuyệt đối không bypass
 * cơ chế bảo vệ hay tải asset trái phép.
 */
public interface SourceProvider {

    boolean supports(String url);

    SourceAnalysis analyze(String url, String userContext);
}
