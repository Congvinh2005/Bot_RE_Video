package com.aivideo.video;

import com.aivideo.video.dto.GenerateSubtitleRequest;
import com.aivideo.video.dto.SubtitleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{id}")
@RequiredArgsConstructor
public class SubtitleController {

    private final SubtitleService subtitleService;

    @PostMapping("/generate-subtitle")
    public ResponseEntity<SubtitleResponse> generateSubtitle(
            @PathVariable UUID id,
            @Valid @RequestBody GenerateSubtitleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subtitleService.generateSubtitle(id, request.voiceGenerationId()));
    }
}
