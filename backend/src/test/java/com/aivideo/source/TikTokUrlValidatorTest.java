package com.aivideo.source;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class TikTokUrlValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "https://www.tiktok.com/@user/video/7234567890123456789",
            "https://tiktok.com/@user/video/123",
            "https://m.tiktok.com/v/7234567890123456789.html",
            "https://vm.tiktok.com/ZM123abc/",
            "https://vt.tiktok.com/ZS456def/",
            "https://www.tiktok.com/@user/video/123?lang=vi-VN"
    })
    void acceptsTikTokUrls(String url) {
        assertThat(TikTokUrlValidator.isTikTokUrl(url)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://www.youtube.com/watch?v=abc",
            "https://facebook.com/reel/123",
            "https://faketiktok.com/@user/video/123",
            "https://tiktok.evil.com/video/123",
            "not a url",
            "",
            "   "
    })
    void rejectsNonTikTokUrls(String url) {
        assertThat(TikTokUrlValidator.isTikTokUrl(url)).isFalse();
    }

    @Test
    void rejectsNull() {
        assertThat(TikTokUrlValidator.isTikTokUrl(null)).isFalse();
    }
}
