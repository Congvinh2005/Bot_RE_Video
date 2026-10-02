package com.aivideo.video;

import com.aivideo.video.dto.GenerateVideoRequest;
import com.aivideo.video.dto.VideoResultResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{id}")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @PostMapping("/generate-video")
    public ResponseEntity<VideoResultResponse> generateVideo(
            @PathVariable UUID id,
            @Valid @RequestBody GenerateVideoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(videoService.generateVideo(id, request));
    }
}
