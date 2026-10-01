package com.aivideo.content;

import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.content.dto.ContentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContentController.class)
class ContentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    ContentService contentService;

    private ContentResponse sample(UUID projectId) {
        return new ContentResponse(UUID.randomUUID(), projectId, 1, "Hook!", "Script",
                "Caption", List.of("#f"), "Mua",
                List.of(Map.of("start", 0, "end", 3, "voiceText", "V")), Instant.now());
    }

    @Test
    void generateReturns201WithScenes() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(contentService.generateContent(eq(projectId), any())).thenReturn(sample(projectId));

        mockMvc.perform(post("/api/projects/{id}/generate-content", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("userContext", "ctx"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hook").value("Hook!"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.scenes.length()").value(1));
    }

    @Test
    void generateWithoutBodyWorks() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(contentService.generateContent(eq(projectId), any())).thenReturn(sample(projectId));

        mockMvc.perform(post("/api/projects/{id}/generate-content", projectId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hook").value("Hook!"));
    }

    @Test
    void generateMissingProjectReturns404() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(contentService.generateContent(eq(projectId), any())).thenThrow(
                new ResourceNotFoundException("PROJECT_NOT_FOUND", "missing"));

        mockMvc.perform(post("/api/projects/{id}/generate-content", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }
}
