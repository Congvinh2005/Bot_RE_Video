package com.aivideo.source;

import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.project.Project;
import com.aivideo.project.ProjectRepository;
import com.aivideo.project.ProjectStatus;
import com.aivideo.project.WorkflowType;
import com.aivideo.source.dto.TikTokRequest;
import com.aivideo.source.dto.TikTokResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TikTokServiceTest {

    @Mock
    ProjectRepository projectRepository;
    @Mock
    VideoSourceRepository videoSourceRepository;
    @Mock
    SourceProvider tikTokProvider;

    TikTokService tikTokService;

    UUID projectId;
    Project project;

    @BeforeEach
    void setUp() {
        tikTokService = new TikTokService(projectRepository, videoSourceRepository,
                List.of(tikTokProvider));
        projectId = UUID.randomUUID();
        project = Project.builder()
                .name("P").workflowType(WorkflowType.TIKTOK_PRODUCT)
                .status(ProjectStatus.CREATED).build();
        project.setId(projectId);
    }

    private void mockSupports() {
        when(tikTokProvider.supports(any())).thenAnswer(inv ->
                TikTokUrlValidator.isTikTokUrl(inv.getArgument(0)));
    }

    private void mockSaveSource() {
        when(videoSourceRepository.save(any(VideoSource.class))).thenAnswer(inv -> {
            VideoSource s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
    }

    @Test
    void analyzeCreatesTikTokSourceWithMetadata() {
        mockSupports();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(tikTokProvider.analyze(any(), any())).thenReturn(new SourceAnalysis(
                SourceStatus.USER_UPLOAD_REQUIRED,
                new SourceMetadata("T", "A", "http://a", "http://t"), "msg"));
        mockSaveSource();

        TikTokResponse response = tikTokService.analyzeTikTok(projectId,
                new TikTokRequest("https://www.tiktok.com/@u/video/123", "ctx"));

        assertThat(response.status()).isEqualTo(SourceStatus.USER_UPLOAD_REQUIRED);
        assertThat(response.metadata().author()).isEqualTo("A");
        assertThat(response.videoSource().sourceType()).isEqualTo(SourceType.TIKTOK);
        assertThat(response.videoSource().status()).isEqualTo(SourceStatus.USER_UPLOAD_REQUIRED);
    }

    @Test
    void analyzeWithoutMetadataStillRequiresUpload() {
        mockSupports();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(tikTokProvider.analyze(any(), any())).thenReturn(
                new SourceAnalysis(SourceStatus.USER_UPLOAD_REQUIRED, null, "upload please"));
        mockSaveSource();

        TikTokResponse response = tikTokService.analyzeTikTok(projectId,
                new TikTokRequest("https://vm.tiktok.com/abc/", null));

        assertThat(response.status()).isEqualTo(SourceStatus.USER_UPLOAD_REQUIRED);
        assertThat(response.metadata()).isNull();
    }

    @Test
    void analyzeRejectsInvalidUrl() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> tikTokService.analyzeTikTok(projectId,
                new TikTokRequest("https://youtube.com/watch?v=1", null)))
                .isInstanceOf(VideoUploadException.class)
                .satisfies(ex -> assertThat(((VideoUploadException) ex).getCode())
                        .isEqualTo("INVALID_URL"));
    }

    @Test
    void analyzeMissingProjectThrowsNotFound() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tikTokService.analyzeTikTok(projectId,
                new TikTokRequest("https://www.tiktok.com/@u/video/123", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
