package com.aivideo.voice;

import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.content.ContentGeneration;
import com.aivideo.content.ContentGenerationRepository;
import com.aivideo.media.MediaStorage;
import com.aivideo.project.Project;
import com.aivideo.project.ProjectRepository;
import com.aivideo.project.ProjectStatus;
import com.aivideo.project.WorkflowType;
import com.aivideo.voice.dto.GenerateVoiceRequest;
import com.aivideo.voice.dto.VoiceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoiceServiceTest {

    @Mock
    ProjectRepository projectRepository;
    @Mock
    ContentGenerationRepository contentGenerationRepository;
    @Mock
    VoiceGenerationRepository voiceGenerationRepository;
    @Mock
    VoiceProvider voiceProvider;
    @Mock
    MediaStorage mediaStorage;

    VoiceService service;

    UUID projectId;
    UUID contentId;
    Project project;
    ContentGeneration content;

    @BeforeEach
    void setUp() {
        service = new VoiceService(projectRepository, contentGenerationRepository,
                voiceGenerationRepository, voiceProvider, mediaStorage);
        projectId = UUID.randomUUID();
        contentId = UUID.randomUUID();
        project = Project.builder().name("P")
                .workflowType(WorkflowType.NORMAL_VIDEO).status(ProjectStatus.CREATED).build();
        project.setId(projectId);
        content = ContentGeneration.builder().project(project).version(1)
                .script("Xin chao cac ban").build();
        content.setId(contentId);
    }

    private void mockHappyPath() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(contentGenerationRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(voiceProvider.voiceName()).thenReturn("ADAM");
        when(voiceProvider.generateSpeech(eq("Xin chao cac ban"), any()))
                .thenReturn(new byte[]{9, 9, 9});
        when(voiceGenerationRepository.save(any(VoiceGeneration.class))).thenAnswer(inv -> {
            VoiceGeneration v = inv.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });
        when(mediaStorage.getUrl(anyString())).thenReturn("http://cdn/audio.mp3");
    }

    @Test
    void generateVoiceUploadsAudioAndSaves() {
        mockHappyPath();

        VoiceResponse response = service.generateVoice(projectId,
                new GenerateVoiceRequest(contentId, "ADAM", 1.0, 0.0, "natural"));

        assertThat(response.voice()).isEqualTo("ADAM");
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.storageKey()).startsWith("audio/");
        assertThat(response.audioUrl()).isEqualTo("http://cdn/audio.mp3");
        assertThat(response.contentGenerationId()).isEqualTo(contentId);
        verify(mediaStorage).upload(anyString(), any(), eq(3L), eq("audio/mpeg"));
    }

    @Test
    void generateVoiceUsesDefaultsWhenOptionsMissing() {
        mockHappyPath();

        VoiceResponse response = service.generateVoice(projectId,
                new GenerateVoiceRequest(contentId, null, null, null, null));

        assertThat(response.voice()).isEqualTo("ADAM");
        assertThat(response.speed()).isEqualTo(1.0);
    }

    @Test
    void generateVoiceRejectsEmptyScript() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        ContentGeneration empty = ContentGeneration.builder().project(project)
                .version(1).script("  ").build();
        empty.setId(contentId);
        when(contentGenerationRepository.findById(contentId)).thenReturn(Optional.of(empty));

        assertThatThrownBy(() -> service.generateVoice(projectId,
                new GenerateVoiceRequest(contentId, "ADAM", 1.0, 0.0, "natural")))
                .isInstanceOf(VoiceException.class);
    }

    @Test
    void generateVoiceMissingProjectThrows() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateVoice(projectId,
                new GenerateVoiceRequest(contentId, "ADAM", 1.0, 0.0, "natural")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void generateVoiceMissingContentThrows() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(contentGenerationRepository.findById(contentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateVoice(projectId,
                new GenerateVoiceRequest(contentId, "ADAM", 1.0, 0.0, "natural")))
                .isInstanceOf(ResourceNotFoundException.class)
                .satisfies(ex -> assertThat(((ResourceNotFoundException) ex).getCode())
                        .isEqualTo("CONTENT_NOT_FOUND"));
    }

    @Test
    void generateVoiceRejectsOtherProjectContent() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        Project other = Project.builder().name("O")
                .workflowType(WorkflowType.NORMAL_VIDEO).status(ProjectStatus.CREATED).build();
        other.setId(UUID.randomUUID());
        ContentGeneration foreign = ContentGeneration.builder().project(other)
                .version(1).script("hi").build();
        foreign.setId(contentId);
        when(contentGenerationRepository.findById(contentId)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.generateVoice(projectId,
                new GenerateVoiceRequest(contentId, "ADAM", 1.0, 0.0, "natural")))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
