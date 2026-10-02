package com.aivideo.result;

import com.aivideo.content.dto.ContentResponse;
import com.aivideo.product.dto.ProductResponse;
import com.aivideo.video.dto.VideoResultResponse;
import com.aivideo.voice.dto.VoiceResponse;

public record ProjectResultResponse(
        ContentResponse content,
        VoiceResponse voice,
        VideoResultResponse video,
        ProductResponse product
) {
}
