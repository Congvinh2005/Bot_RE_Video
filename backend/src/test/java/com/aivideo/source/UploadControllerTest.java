package com.aivideo.source;

import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.source.dto.VideoSourceResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import com.aivideo.auth.JwtService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(UploadController.class)
class UploadControllerTest {

    @MockBean
    JwtService jwtService;

    @Autowired
    MockMvc mockMvc;

    @MockBean
    UploadService uploadService;

    private VideoSourceResponse sample(UUID projectId) {
        return new VideoSourceResponse(UUID.randomUUID(), projectId, SourceType.UPLOAD,
                "product.mp4", "video/abc.mp4", 15.2, 1080, 1920, 30.0,
                "h264", true, "mp4", SourceStatus.READY, Instant.now());
    }

    @Test
    void uploadReturns201WithMetadata() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(uploadService.uploadVideo(eq(projectId), any())).thenReturn(sample(projectId));
        MockMultipartFile file = new MockMultipartFile("file", "product.mp4",
                "video/mp4", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/projects/{id}/upload", projectId)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename").value("product.mp4"))
                .andExpect(jsonPath("$.duration").value(15.2))
                .andExpect(jsonPath("$.width").value(1080))
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    void uploadMissingProjectReturns404() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(uploadService.uploadVideo(eq(projectId), any())).thenThrow(
                new ResourceNotFoundException("PROJECT_NOT_FOUND", "Project not found"));
        MockMultipartFile file = new MockMultipartFile("file", "a.mp4",
                "video/mp4", new byte[]{1});

        mockMvc.perform(multipart("/api/projects/{id}/upload", projectId).file(file))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void uploadInvalidFormatReturns400() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(uploadService.uploadVideo(eq(projectId), any())).thenThrow(
                new VideoUploadException("VIDEO_FORMAT_INVALID", "Unsupported",
                        HttpStatus.BAD_REQUEST));
        MockMultipartFile file = new MockMultipartFile("file", "a.avi",
                "video/mp4", new byte[]{1});

        mockMvc.perform(multipart("/api/projects/{id}/upload", projectId).file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VIDEO_FORMAT_INVALID"));
    }

    @Test
    void uploadTooLargeReturns413() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(uploadService.uploadVideo(eq(projectId), any())).thenThrow(
                new VideoUploadException("FILE_TOO_LARGE", "Too large",
                        HttpStatus.PAYLOAD_TOO_LARGE));
        MockMultipartFile file = new MockMultipartFile("file", "a.mp4",
                "video/mp4", new byte[]{1});

        mockMvc.perform(multipart("/api/projects/{id}/upload", projectId).file(file))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
    }
}
