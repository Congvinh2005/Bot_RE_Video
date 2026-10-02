package com.aivideo.job;

import com.aivideo.project.Project;
import com.aivideo.project.ProjectStatus;
import com.aivideo.project.WorkflowType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobController.class)
class JobControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    JobOrchestrator jobOrchestrator;
    @MockBean
    JobRunner jobRunner;
    @MockBean
    VideoJobRepository videoJobRepository;

    private VideoJob jobFor(UUID projectId, JobStatus status, int progress) {
        Project project = Project.builder().name("P")
                .workflowType(WorkflowType.NORMAL_VIDEO).status(ProjectStatus.PROCESSING).build();
        project.setId(projectId);
        VideoJob job = VideoJob.builder().project(project)
                .status(status).progress(progress)
                .currentStep(JobStep.VIDEO).build();
        job.setId(UUID.randomUUID());
        return job;
    }

    @Test
    void generateFullReturns202AndTriggersRunner() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        VideoJob job = VideoJob.builder().status(JobStatus.PENDING).progress(0).build();
        job.setId(jobId);
        when(jobOrchestrator.launchFullPipeline(eq(projectId), any())).thenReturn(job);

        mockMvc.perform(post("/api/projects/{id}/generate-full", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("userContext", "ctx"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(jobRunner).run(jobId, "ctx");
    }

    @Test
    void statusReturnsProgress() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(videoJobRepository.findByProjectIdOrderByCreatedAtDesc(projectId))
                .thenReturn(List.of(jobFor(projectId, JobStatus.PROCESSING, 80)));

        mockMvc.perform(get("/api/projects/{id}/status", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress").value(80))
                .andExpect(jsonPath("$.currentStep").value("VIDEO"));
    }

    @Test
    void statusWithoutJobReturns404() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(videoJobRepository.findByProjectIdOrderByCreatedAtDesc(projectId))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/projects/{id}/status", projectId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("JOB_NOT_FOUND"));
    }

    @Test
    void statusStreamEmitsTerminalStatus() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(videoJobRepository.findByProjectIdOrderByCreatedAtDesc(projectId))
                .thenReturn(List.of(jobFor(projectId, JobStatus.COMPLETED, 100)));

        MvcResult mvcResult = mockMvc.perform(
                        get("/api/projects/{id}/status/stream", projectId)
                                .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk());
        assertThat(mvcResult.getResponse().getContentAsString()).contains("COMPLETED");
    }
}
