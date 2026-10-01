package com.aivideo.voice;

import com.aivideo.voice.dto.GenerateVoiceRequest;
import com.aivideo.voice.dto.VoiceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{id}")
@RequiredArgsConstructor
public class VoiceController {

    private final VoiceService voiceService;

    @PostMapping("/generate-voice")
    public ResponseEntity<VoiceResponse> generateVoice(
            @PathVariable UUID id,
            @Valid @RequestBody GenerateVoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(voiceService.generateVoice(id, request));
    }
}
