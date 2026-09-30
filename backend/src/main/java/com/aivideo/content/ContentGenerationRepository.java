package com.aivideo.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContentGenerationRepository extends JpaRepository<ContentGeneration, UUID> {
    List<ContentGeneration> findByProjectIdOrderByVersionDesc(UUID projectId);
}
