package com.aivideo.source;

import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.media.MediaAssetRepository;
import com.aivideo.media.MediaStorage;
import com.aivideo.project.Project;
import com.aivideo.project.ProjectRepository;
import com.aivideo.project.ProjectStatus;
import com.aivideo.project.WorkflowType;
import com.aivideo.source.dto.VideoSourceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadServiceTest {

    @Mock
    ProjectRepository projectRepository;
    @Mock
    VideoSourceRepository videoSourceRepository;
    @Mock
    MediaAssetRepository mediaAssetRepository;
    @Mock
    MediaStorage mediaStorage;
    @Mock
    VideoMetadataService metadataService;

    UploadService uploadService;

    UUID projectId;
    Project project;

    @BeforeEach
    void setUp() {
        UploadProperties props = new UploadProperties(500,
                List.of("mp4", "mov", "webm"),
                List.of("video/mp4", "video/quicktime", "video/webm"),
                "ffprobe");
        uploadService = new UploadService(projectRepository, videoSourceRepository,
                mediaAssetRepository, mediaStorage, metadataService, props);
        projectId = UUID.randomUUID();
        project = Project.builder()
                .name("P").workflowType(WorkflowType.NORMAL_VIDEO)
                .status(ProjectStatus.CREATED).build();
        project.setId(projectId);
    }

    private MockMultipartFile videoFile(String name, String contentType, byte[] content) {
        return new MockMultipartFile("file", name, contentType, content);
    }

    @Test
    void uploadHappyPathCreatesSourceAndAsset() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(metadataService.probe(any(Path.class))).thenReturn(
                new VideoMetadata(15.2, 1080, 1920, 30.0, "h264", "aac", true, "mp4"));
        when(videoSourceRepository.save(any(VideoSource.class))).thenAnswer(inv -> {
            VideoSource s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        VideoSourceResponse response = uploadService.uploadVideo(projectId,
                videoFile("product.mp4", "video/mp4", new byte[]{1, 2, 3}));

        assertThat(response.projectId()).isEqualTo(projectId);
        assertThat(response.sourceType()).isEqualTo(SourceType.UPLOAD);
        assertThat(response.status()).isEqualTo(SourceStatus.READY);
        assertThat(response.duration()).isEqualTo(15.2);
        assertThat(response.width()).isEqualTo(1080);
        assertThat(response.storageKey()).startsWith("video/");
        verify(mediaStorage).upload(any(), any(), anyLong(), eq("video/mp4"));
        verify(mediaAssetRepository).save(any());
    }

    @Test
    void uploadMissingProjectThrowsNotFound() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> uploadService.uploadVideo(projectId,
                videoFile("a.mp4", "video/mp4", new byte[]{1})))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(mediaStorage);
    }

    @Test
    void uploadEmptyFileThrows() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> uploadService.uploadVideo(projectId,
                videoFile("a.mp4", "video/mp4", new byte[]{})))
                .isInstanceOf(VideoUploadException.class)
                .satisfies(ex -> assertThat(((VideoUploadException) ex).getCode())
                        .isEqualTo("UPLOAD_FAILED"));
    }

    @Test
    void uploadUnsupportedExtensionThrows() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> uploadService.uploadVideo(projectId,
                videoFile("a.avi", "video/mp4", new byte[]{1})))
                .isInstanceOf(VideoUploadException.class)
                .satisfies(ex -> assertThat(((VideoUploadException) ex).getCode())
                        .isEqualTo("VIDEO_FORMAT_INVALID"));
    }

    @Test
    void uploadUnsupportedMimeThrows() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> uploadService.uploadVideo(projectId,
                videoFile("a.mp4", "application/octet-stream", new byte[]{1})))
                .isInstanceOf(VideoUploadException.class)
                .satisfies(ex -> assertThat(((VideoUploadException) ex).getCode())
                        .isEqualTo("VIDEO_FORMAT_INVALID"));
    }

    @Test
    void uploadOversizeThrowsFileTooLarge() {
        UploadProperties small = new UploadProperties(1,
                List.of("mp4", "mov", "webm"),
                List.of("video/mp4", "video/quicktime", "video/webm"),
                "ffprobe");
        UploadService smallService = new UploadService(projectRepository, videoSourceRepository,
                mediaAssetRepository, mediaStorage, metadataService, small);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> smallService.uploadVideo(projectId,
                videoFile("a.mp4", "video/mp4", new byte[2 * 1024 * 1024])))
                .isInstanceOf(VideoUploadException.class)
                .satisfies(ex -> assertThat(((VideoUploadException) ex).getCode())
                        .isEqualTo("FILE_TOO_LARGE"));
    }
}
