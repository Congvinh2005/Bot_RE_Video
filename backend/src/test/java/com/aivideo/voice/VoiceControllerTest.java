package com.aivideo.voice;

import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.voice.dto.VoiceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VoiceController.class)
class VoiceControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    VoiceService voiceService;

    private VoiceResponse sample(UUID projectId, UUID contentId) {
        return new VoiceResponse(UUID.randomUUID(), projectId, contentId, "ADAM",
                1.0, 0.0, "natural", "vi", "mp3", "audio/x.mp3",
                "http://cdn/x.mp3", "COMPLETED", Instant.now());
    }

    @Test
    void generateReturns201WithAudioUrl() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        when(voiceService.generateVoice(eq(projectId), any())).thenReturn(sample(projectId, contentId));

        mockMvc.perform(post("/api/projects/{id}/generate-voice", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "contentGenerationId", contentId.toString(),
                                "voice", "ADAM",
                                "speed", 1.0,
                                "pitch", 0.0,
                                "emotion", "natural"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.voice").value("ADAM"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.audioUrl").value("http://cdn/x.mp3"));
    }

    @Test
    void generateMissingContentIdFailsValidation() throws Exception {
        UUID projectId = UUID.randomUUID();

        mockMvc.perform(post("/api/projects/{id}/generate-voice", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("voice", "ADAM"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void generateTtsErrorReturns502() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        when(voiceService.generateVoice(eq(projectId), any())).thenThrow(
                new VoiceException("TTS_PROVIDER_ERROR", "down"));

        mockMvc.perform(post("/api/projects/{id}/generate-voice", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("contentGenerationId", contentId.toString()))))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("TTS_PROVIDER_ERROR"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void generateMissingProjectReturns404() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        when(voiceService.generateVoice(eq(projectId), any())).thenThrow(
                new ResourceNotFoundException("PROJECT_NOT_FOUND", "missing"));

        mockMvc.perform(post("/api/projects/{id}/generate-voice", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("contentGenerationId", contentId.toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }
}
