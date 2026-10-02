package com.aivideo.video;

import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.content.ContentGeneration;
import com.aivideo.media.MediaAssetRepository;
import com.aivideo.media.MediaStorage;
import com.aivideo.project.Project;
import com.aivideo.project.ProjectRepository;
import com.aivideo.project.ProjectStatus;
import com.aivideo.project.WorkflowType;
import com.aivideo.source.VideoMetadata;
import com.aivideo.source.VideoMetadataService;
import com.aivideo.video.dto.SubtitleResponse;
import com.aivideo.voice.VoiceGeneration;
import com.aivideo.voice.VoiceGenerationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubtitleServiceTest {

    @Mock
    ProjectRepository projectRepository;
    @Mock
    VoiceGenerationRepository voiceGenerationRepository;
    @Mock
    MediaAssetRepository mediaAssetRepository;
    @Mock
    MediaStorage mediaStorage;
    @Mock
    VideoMetadataService metadataService;

    SubtitleService service;

    UUID projectId;
    UUID voiceId;
    Project project;
    VoiceGeneration voice;

    @BeforeEach
    void setUp() {
        service = new SubtitleService(projectRepository, voiceGenerationRepository,
                mediaAssetRepository, mediaStorage, metadataService,
                new SubtitleGenerator(new SubtitleProperties(42)));
        projectId = UUID.randomUUID();
        voiceId = UUID.randomUUID();
        project = Project.builder().name("P")
                .workflowType(WorkflowType.NORMAL_VIDEO).status(ProjectStatus.CREATED).build();
        project.setId(projectId);
        ContentGeneration content = ContentGeneration.builder().project(project).version(1)
                .script("Bo ngu mem mat. Mua ngay hom nay!").build();
        voice = VoiceGeneration.builder().project(project).contentGeneration(content)
                .voice("ADAM").storageKey("audio/v.mp3").status("COMPLETED").build();
        voice.setId(voiceId);
    }

    private void mockVoice() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(voiceGenerationRepository.findById(voiceId)).thenReturn(Optional.of(voice));
        when(mediaStorage.download("audio/v.mp3"))
                .thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
    }

    @Test
    void generateSubtitleUploadsSrtSyncedToAudio() throws Exception {
        mockVoice();
        when(metadataService.probe(any())).thenReturn(
                new VideoMetadata(12.0, null, null, null, null, "mp3", true, "mp3"));
        when(mediaStorage.getUrl(anyString())).thenReturn("http://cdn/sub.srt");

        SubtitleResponse response = service.generateSubtitle(projectId, voiceId);

        assertThat(response.storageKey()).startsWith("subtitle/");
        assertThat(response.subtitleUrl()).isEqualTo("http://cdn/sub.srt");
        assertThat(response.cueCount()).isGreaterThan(0);
        assertThat(response.audioDurationSeconds()).isEqualTo(12.0);
        verify(mediaStorage).upload(anyString(), any(), any(Long.class), anyString());
        verify(mediaAssetRepository).save(any());
    }

    @Test
    void generateSubtitleMissingVoiceThrows() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(voiceGenerationRepository.findById(voiceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateSubtitle(projectId, voiceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .satisfies(ex -> assertThat(((ResourceNotFoundException) ex).getCode())
                        .isEqualTo("VOICE_NOT_FOUND"));
    }

    @Test
    void generateSubtitleUnknownDurationThrows() throws Exception {
        mockVoice();
        when(metadataService.probe(any())).thenReturn(
                new VideoMetadata(null, null, null, null, null, "mp3", true, "mp3"));

        assertThatThrownBy(() -> service.generateSubtitle(projectId, voiceId))
                .isInstanceOf(SubtitleException.class)
                .satisfies(ex -> assertThat(((SubtitleException) ex).getCode())
                        .isEqualTo("SUBTITLE_FAILED"));
    }
}
