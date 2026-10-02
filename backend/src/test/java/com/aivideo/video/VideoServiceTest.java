package com.aivideo.video;

import com.aivideo.analysis.FrameExtractor;
import com.aivideo.common.exception.BadRequestException;
import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.content.ContentGeneration;
import com.aivideo.content.ContentGenerationRepository;
import com.aivideo.media.MediaAssetRepository;
import com.aivideo.media.MediaStorage;
import com.aivideo.project.Project;
import com.aivideo.project.ProjectRepository;
import com.aivideo.project.ProjectStatus;
import com.aivideo.project.WorkflowType;
import com.aivideo.source.SourceStatus;
import com.aivideo.source.SourceType;
import com.aivideo.source.VideoMetadata;
import com.aivideo.source.VideoMetadataService;
import com.aivideo.source.VideoSource;
import com.aivideo.source.VideoSourceRepository;
import com.aivideo.video.dto.GenerateVideoRequest;
import com.aivideo.video.dto.VideoResultResponse;
import com.aivideo.voice.VoiceGeneration;
import com.aivideo.voice.VoiceGenerationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoServiceTest {

    @Mock
    ProjectRepository projectRepository;
    @Mock
    ContentGenerationRepository contentGenerationRepository;
    @Mock
    VoiceGenerationRepository voiceGenerationRepository;
    @Mock
    VideoSourceRepository videoSourceRepository;
    @Mock
    GeneratedVideoRepository generatedVideoRepository;
    @Mock
    MediaAssetRepository mediaAssetRepository;
    @Mock
    MediaStorage mediaStorage;
    @Mock
    VideoMetadataService metadataService;
    @Mock
    FrameExtractor frameExtractor;
    @Mock
    SubtitleGenerator subtitleGenerator;
    @Mock
    VideoComposer videoComposer;

    @TempDir
    Path tempDir;

    VideoService service;

    UUID projectId;
    UUID contentId;
    UUID voiceId;
    Project project;
    ContentGeneration content;
    VoiceGeneration voice;
    VideoSource source;

    @BeforeEach
    void setUp() {
        service = new VideoService(projectRepository, contentGenerationRepository,
                voiceGenerationRepository, videoSourceRepository, generatedVideoRepository,
                mediaAssetRepository, mediaStorage, metadataService, frameExtractor,
                subtitleGenerator, videoComposer);
        projectId = UUID.randomUUID();
        contentId = UUID.randomUUID();
        voiceId = UUID.randomUUID();
        project = Project.builder().name("P")
                .workflowType(WorkflowType.NORMAL_VIDEO).status(ProjectStatus.CREATED).build();
        project.setId(projectId);
        content = ContentGeneration.builder().project(project).version(1)
                .script("Hello world")
                .scenes(List.of(Map.of("start", 0, "end", 3,
                        "voiceText", "Hi", "overlayText", "Mua ngay")))
                .build();
        content.setId(contentId);
        voice = VoiceGeneration.builder().project(project).contentGeneration(content)
                .voice("ADAM").storageKey("audio/v.mp3").status("COMPLETED").build();
        voice.setId(voiceId);
        source = VideoSource.builder().project(project).sourceType(SourceType.UPLOAD)
                .originalFilename("a.mp4").storageKey("video/a.mp4")
                .status(SourceStatus.READY).build();
    }

    private void mockHappyPath(Path output) throws Exception {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(contentGenerationRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(voiceGenerationRepository.findById(voiceId)).thenReturn(Optional.of(voice));
        when(videoSourceRepository.findByProjectId(projectId)).thenReturn(List.of(source));
        when(mediaStorage.download(anyString()))
                .thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        when(metadataService.probe(any(Path.class))).thenAnswer(inv -> {
            Path file = inv.getArgument(0);
            if (file.toString().endsWith("voice.mp3")) {
                return new VideoMetadata(10.0, null, null, null, null, "mp3", true, "mp3");
            }
            return new VideoMetadata(10.0, 1080, 1920, 30.0, "h264", "aac", true, "mp4");
        });
        when(subtitleGenerator.generate(anyString(), any(Double.class))).thenReturn("srt");
        when(videoComposer.compose(any(), any(), any(), any(), any(Double.class)))
                .thenReturn(output);
        when(frameExtractor.extractFrame(any(), any(Double.class)))
                .thenReturn(new byte[]{(byte) 0xFF, (byte) 0xD8});
        when(generatedVideoRepository.save(any(GeneratedVideo.class))).thenAnswer(inv -> {
            GeneratedVideo v = inv.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });
        when(mediaStorage.getUrl(anyString())).thenReturn("http://cdn/x");
    }

    @Test
    void generateVideoSaves1080x1920AndReturnsUrls() throws Exception {
        Path output = tempDir.resolve("final.mp4");
        Files.write(output, new byte[2048]);
        mockHappyPath(output);

        VideoResultResponse response = service.generateVideo(projectId,
                new GenerateVideoRequest(contentId, voiceId));

        assertThat(response.width()).isEqualTo(1080);
        assertThat(response.height()).isEqualTo(1920);
        assertThat(response.videoUrl()).isEqualTo("http://cdn/x");
        assertThat(response.thumbnailUrl()).isEqualTo("http://cdn/x");
        assertThat(response.storageKey()).startsWith("video/");
        verify(mediaAssetRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void generateVideoMissingContentThrows() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(contentGenerationRepository.findById(contentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateVideo(projectId,
                new GenerateVideoRequest(contentId, voiceId)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void generateVideoNotReadyVoiceThrows() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(contentGenerationRepository.findById(contentId)).thenReturn(Optional.of(content));
        VoiceGeneration pending = VoiceGeneration.builder().project(project)
                .status("PENDING").build();
        when(voiceGenerationRepository.findById(voiceId)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.generateVideo(projectId,
                new GenerateVideoRequest(contentId, voiceId)))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getCode())
                        .isEqualTo("VOICE_NOT_READY"));
    }

    @Test
    void generateVideoWithoutUploadableSourceThrows() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(contentGenerationRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(voiceGenerationRepository.findById(voiceId)).thenReturn(Optional.of(voice));
        VideoSource tiktokOnly = VideoSource.builder().project(project)
                .sourceType(SourceType.TIKTOK).status(SourceStatus.USER_UPLOAD_REQUIRED).build();
        when(videoSourceRepository.findByProjectId(projectId)).thenReturn(List.of(tiktokOnly));

        assertThatThrownBy(() -> service.generateVideo(projectId,
                new GenerateVideoRequest(contentId, voiceId)))
                .isInstanceOf(ResourceNotFoundException.class)
                .satisfies(ex -> assertThat(((ResourceNotFoundException) ex).getCode())
                        .isEqualTo("SOURCE_NOT_FOUND"));
    }
}
