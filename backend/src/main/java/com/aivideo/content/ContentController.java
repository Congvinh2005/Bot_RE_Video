package com.aivideo.content;

import com.aivideo.content.dto.ContentResponse;
import com.aivideo.content.dto.GenerateContentRequest;
import com.aivideo.content.dto.UpdateContentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{id}")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @PostMapping("/generate-content")
    public ResponseEntity<ContentResponse> generateContent(
            @PathVariable UUID id,
            @RequestBody(required = false) GenerateContentRequest request) {
        String userContext = request != null ? request.userContext() : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contentService.generateContent(id, userContext));
    }

    @PutMapping("/content")
    public ResponseEntity<ContentResponse> updateContent(
            @PathVariable UUID id,
            @RequestBody UpdateContentRequest request) {
        return ResponseEntity.ok(contentService.updateContent(id, request));
    }
}
