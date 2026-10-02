package com.aivideo.video;

import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.video.dto.SubtitleResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import com.aivideo.auth.JwtService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(SubtitleController.class)
class SubtitleControllerTest {

    @MockBean
    JwtService jwtService;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    SubtitleService subtitleService;

    @Test
    void generateReturns201WithCues() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID voiceId = UUID.randomUUID();
        when(subtitleService.generateSubtitle(eq(projectId), eq(voiceId))).thenReturn(
                new SubtitleResponse("subtitle/x.srt", "http://cdn/x.srt", 4, 12.0));

        mockMvc.perform(post("/api/projects/{id}/generate-subtitle", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("voiceGenerationId", voiceId.toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cueCount").value(4))
                .andExpect(jsonPath("$.subtitleUrl").value("http://cdn/x.srt"));
    }

    @Test
    void generateMissingVoiceIdFailsValidation() throws Exception {
        UUID projectId = UUID.randomUUID();

        mockMvc.perform(post("/api/projects/{id}/generate-subtitle", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void generateMissingVoiceReturns404() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID voiceId = UUID.randomUUID();
        when(subtitleService.generateSubtitle(eq(projectId), eq(voiceId))).thenThrow(
                new ResourceNotFoundException("VOICE_NOT_FOUND", "missing"));

        mockMvc.perform(post("/api/projects/{id}/generate-subtitle", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("voiceGenerationId", voiceId.toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VOICE_NOT_FOUND"));
    }
}
