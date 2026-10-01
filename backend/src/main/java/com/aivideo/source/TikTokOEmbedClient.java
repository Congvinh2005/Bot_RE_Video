package com.aivideo.source;

import java.util.Optional;

/** Lấy metadata được phép qua TikTok oEmbed. Không trả về video asset. */
public interface TikTokOEmbedClient {

    Optional<OEmbedData> fetch(String videoUrl);
}
