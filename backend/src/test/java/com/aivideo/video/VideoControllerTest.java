package com.aivideo.video;

import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.video.dto.VideoResultResponse;
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

import java.time.Instant;
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
@WebMvcTest(VideoController.class)
class VideoControllerTest {

    @MockBean
    JwtService jwtService;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    VideoService videoService;

    private VideoResultResponse sample(UUID projectId) {
        return new VideoResultResponse(UUID.randomUUID(), projectId, "video/final.mp4",
                "http://cdn/final.mp4", "http://cdn/thumb.jpg", 25.0,
                1080, 1920, "COMPLETED", Instant.now());
    }

    @Test
    void generateReturns201WithUrls() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        UUID voiceId = UUID.randomUUID();
        when(videoService.generateVideo(eq(projectId), any())).thenReturn(sample(projectId));

        mockMvc.perform(post("/api/projects/{id}/generate-video", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "contentGenerationId", contentId.toString(),
                                "voiceGenerationId", voiceId.toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.width").value(1080))
                .andExpect(jsonPath("$.height").value(1920))
                .andExpect(jsonPath("$.videoUrl").value("http://cdn/final.mp4"));
    }

    @Test
    void generateMissingIdsFailsValidation() throws Exception {
        UUID projectId = UUID.randomUUID();

        mockMvc.perform(post("/api/projects/{id}/generate-video", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void generateMissingProjectReturns404() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(videoService.generateVideo(eq(projectId), any())).thenThrow(
                new ResourceNotFoundException("PROJECT_NOT_FOUND", "missing"));

        mockMvc.perform(post("/api/projects/{id}/generate-video", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "contentGenerationId", UUID.randomUUID().toString(),
                                "voiceGenerationId", UUID.randomUUID().toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }
}
