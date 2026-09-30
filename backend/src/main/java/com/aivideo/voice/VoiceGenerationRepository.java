package com.aivideo.voice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VoiceGenerationRepository extends JpaRepository<VoiceGeneration, UUID> {
    List<VoiceGeneration> findByProjectId(UUID projectId);
}
