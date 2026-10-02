package com.aivideo.analysis;

import com.aivideo.analysis.dto.AnalyzeJobResponse;
import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.job.JobStatus;
import com.aivideo.job.VideoJob;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AnalysisController.class)
class AnalysisControllerTest {

    @MockBean
    JwtService jwtService;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    AnalysisService analysisService;
    @MockBean
    AnalysisRunner analysisRunner;

    @Test
    void analyzeReturns202AndTriggersRunner() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        VideoJob job = VideoJob.builder().status(JobStatus.PENDING).build();
        job.setId(jobId);
        when(analysisService.launchAnalysis(eq(projectId), any())).thenReturn(job);

        mockMvc.perform(post("/api/projects/{id}/analyze", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(analysisRunner).run(jobId);
    }

    @Test
    void analyzeMissingProjectReturns404() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(analysisService.launchAnalysis(eq(projectId), any())).thenThrow(
                new ResourceNotFoundException("PROJECT_NOT_FOUND", "missing"));

        mockMvc.perform(post("/api/projects/{id}/analyze", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }

    @Test
    void analyzePassesExplicitSourceId() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        VideoJob job = VideoJob.builder().status(JobStatus.PENDING).build();
        job.setId(jobId);
        when(analysisService.launchAnalysis(eq(projectId), eq(sourceId))).thenReturn(job);

        mockMvc.perform(post("/api/projects/{id}/analyze", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("videoSourceId", sourceId.toString()))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()));
    }
}
