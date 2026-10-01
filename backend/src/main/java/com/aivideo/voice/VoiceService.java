package com.aivideo.voice;

import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.content.ContentGeneration;
import com.aivideo.content.ContentGenerationRepository;
import com.aivideo.media.AssetType;
import com.aivideo.media.MediaStorage;
import com.aivideo.media.StoragePath;
import com.aivideo.project.Project;
import com.aivideo.project.ProjectRepository;
import com.aivideo.voice.dto.GenerateVoiceRequest;
import com.aivideo.voice.dto.VoiceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceService {

    private final ProjectRepository projectRepository;
    private final ContentGenerationRepository contentGenerationRepository;
    private final VoiceGenerationRepository voiceGenerationRepository;
    private final VoiceProvider voiceProvider;
    private final MediaStorage mediaStorage;

    @Transactional
    public VoiceResponse generateVoice(UUID projectId, GenerateVoiceRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));
        ContentGeneration content = contentGenerationRepository.findById(request.contentGenerationId())
                .filter(c -> c.getProject().getId().equals(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("CONTENT_NOT_FOUND",
                        "Content generation not found: " + request.contentGenerationId()));
        if (content.getScript() == null || content.getScript().isBlank()) {
            throw new VoiceException("TTS_PROVIDER_ERROR", "Content script is empty");
        }

        VoiceOptions options = resolveOptions(request);
        if (!voiceProvider.voiceName().equalsIgnoreCase(options.voice())) {
            log.warn("TTS_VOICE_FALLBACK requested={} using={}",
                    options.voice(), voiceProvider.voiceName());
        }
        byte[] audio = voiceProvider.generateSpeech(content.getScript(), options);

        String key = StoragePath.pathFor(AssetType.AUDIO, "voice.mp3");
        mediaStorage.upload(key, new ByteArrayInputStream(audio), audio.length, "audio/mpeg");

        VoiceGeneration saved = voiceGenerationRepository.save(VoiceGeneration.builder()
                .project(project)
                .contentGeneration(content)
                .voice(voiceProvider.voiceName())
                .speed(options.speed())
                .pitch(options.pitch())
                .emotion(options.emotion())
                .language(options.language())
                .format(options.format())
                .storageKey(key)
                .status("COMPLETED")
                .build());
        log.info("TTS_GENERATION_OK project={} voice={} bytes={}",
                projectId, saved.getVoice(), audio.length);
        return VoiceResponse.from(saved, mediaStorage.getUrl(key));
    }

    private VoiceOptions resolveOptions(GenerateVoiceRequest request) {
        VoiceOptions defaults = VoiceOptions.defaults();
        return new VoiceOptions(
                request.voice() != null && !request.voice().isBlank()
                        ? request.voice() : defaults.voice(),
                request.speed() != null ? request.speed() : defaults.speed(),
                request.pitch() != null ? request.pitch() : defaults.pitch(),
                request.emotion() != null && !request.emotion().isBlank()
                        ? request.emotion() : defaults.emotion(),
                defaults.language(),
                defaults.format());
    }
}
