package com.aivideo.project;

import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.project.dto.ProjectResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import com.aivideo.auth.JwtService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    @MockBean
    JwtService jwtService;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    ProjectService projectService;

    private ProjectResponse sample() {
        return new ProjectResponse(UUID.randomUUID(), "My video",
                WorkflowType.NORMAL_VIDEO, ProjectStatus.CREATED,
                Instant.now(), Instant.now());
    }

    @Test
    void createProjectReturns201() throws Exception {
        ProjectResponse response = sample();
        when(projectService.createProject(any())).thenReturn(response);

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "My video", "workflowType", "NORMAL_VIDEO"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.id().toString()))
                .andExpect(jsonPath("$.name").value("My video"))
                .andExpect(jsonPath("$.workflowType").value("NORMAL_VIDEO"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void createProjectRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "  ", "workflowType", "NORMAL_VIDEO"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void createProjectRejectsUnknownWorkflowType() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "My video", "workflowType", "NOPE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void listProjectsReturns200() throws Exception {
        when(projectService.listProjects()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getProjectReturns404WithStandardError() throws Exception {
        UUID id = UUID.randomUUID();
        when(projectService.getProject(id))
                .thenThrow(new ResourceNotFoundException("PROJECT_NOT_FOUND", "Project not found: " + id));

        mockMvc.perform(get("/api/projects/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void deleteProjectReturns204() throws Exception {
        mockMvc.perform(delete("/api/projects/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteMissingProjectReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("PROJECT_NOT_FOUND", "Project not found: " + id))
                .when(projectService).deleteProject(id);

        mockMvc.perform(delete("/api/projects/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }
}
