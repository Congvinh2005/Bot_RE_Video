package com.aivideo.source;

import com.aivideo.source.dto.TikTokRequest;
import com.aivideo.source.dto.TikTokResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{id}")
@RequiredArgsConstructor
public class TikTokController {

    private final TikTokService tikTokService;

    @PostMapping("/tiktok")
    public ResponseEntity<TikTokResponse> analyzeTikTok(
            @PathVariable UUID id,
            @Valid @RequestBody TikTokRequest request) {
        return ResponseEntity.ok(tikTokService.analyzeTikTok(id, request));
    }
}
