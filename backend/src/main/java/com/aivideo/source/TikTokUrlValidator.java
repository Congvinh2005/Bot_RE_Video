package com.aivideo.source;

import java.net.URI;

/** Validate URL TikTok (tiktok.com, vm/vt short link...). Không fetch gì ở đây. */
public final class TikTokUrlValidator {

    private TikTokUrlValidator() {
    }

    public static boolean isTikTokUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            String host = URI.create(url.trim()).getHost();
            if (host == null) {
                return false;
            }
            host = host.toLowerCase();
            return host.equals("tiktok.com") || host.endsWith(".tiktok.com");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
