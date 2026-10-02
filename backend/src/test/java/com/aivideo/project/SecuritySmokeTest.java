package com.aivideo.project;

import com.aivideo.auth.JwtAuthFilter;
import com.aivideo.auth.JwtService;
import com.aivideo.config.SecurityConfig;
import com.aivideo.project.dto.ProjectResponse;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Security giữ nguyên filter: không token -> 401, token hợp lệ -> 200. */
@WebMvcTest(ProjectController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class SecuritySmokeTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ProjectService projectService;

    @MockBean
    JwtService jwtService;

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    void protectedEndpointWithValidTokenReturns200() throws Exception {
        when(jwtService.parseToken("good-token")).thenReturn(
                Jwts.claims().subject("a@b.com").add("role", "USER").build());
        when(projectService.listProjects()).thenReturn(List.of(
                new ProjectResponse(UUID.randomUUID(), "P",
                        WorkflowType.NORMAL_VIDEO, ProjectStatus.CREATED,
                        Instant.now(), Instant.now())));

        mockMvc.perform(get("/api/projects").header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk());
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        when(jwtService.parseToken(anyString())).thenThrow(
                new com.aivideo.common.exception.BadRequestException(
                        "INVALID_TOKEN", "Invalid or expired token"));

        mockMvc.perform(get("/api/projects").header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized());
    }
}
