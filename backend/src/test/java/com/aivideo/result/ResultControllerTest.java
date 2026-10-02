package com.aivideo.result;

import com.aivideo.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResultController.class)
class ResultControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    ResultService resultService;

    @Test
    void getResultReturnsLatestPieces() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(resultService.getResult(eq(projectId)))
                .thenReturn(new ProjectResultResponse(null, null, null, null));

        mockMvc.perform(get("/api/projects/{id}/result", projectId))
                .andExpect(status().isOk());
    }

    @Test
    void getResultMissingProjectReturns404() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(resultService.getResult(eq(projectId))).thenThrow(
                new ResourceNotFoundException("PROJECT_NOT_FOUND", "missing"));

        mockMvc.perform(get("/api/projects/{id}/result", projectId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }
}
