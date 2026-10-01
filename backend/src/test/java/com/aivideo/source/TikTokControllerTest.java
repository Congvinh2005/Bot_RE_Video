package com.aivideo.source;

import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.source.dto.TikTokResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TikTokController.class)
class TikTokControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    TikTokService tikTokService;

    @Test
    void analyzeReturnsUploadRequired() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(tikTokService.analyzeTikTok(eq(projectId), any())).thenReturn(
                new TikTokResponse(SourceStatus.USER_UPLOAD_REQUIRED,
                        "Please upload", null, null));

        mockMvc.perform(post("/api/projects/{id}/tiktok", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "url", "https://www.tiktok.com/@u/video/123",
                                "context", "ctx"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("USER_UPLOAD_REQUIRED"))
                .andExpect(jsonPath("$.message").value("Please upload"));
    }

    @Test
    void analyzeInvalidUrlReturns400() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(tikTokService.analyzeTikTok(eq(projectId), any())).thenThrow(
                new VideoUploadException("INVALID_URL", "Not valid", HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/api/projects/{id}/tiktok", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("url", "https://x.com/1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_URL"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void analyzeMissingProjectReturns404() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(tikTokService.analyzeTikTok(eq(projectId), any())).thenThrow(
                new ResourceNotFoundException("PROJECT_NOT_FOUND", "missing"));

        mockMvc.perform(post("/api/projects/{id}/tiktok", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("url", "https://www.tiktok.com/@u/video/1"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }

    @Test
    void analyzeBlankUrlFailsValidation() throws Exception {
        UUID projectId = UUID.randomUUID();

        mockMvc.perform(post("/api/projects/{id}/tiktok", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("url", "  "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
