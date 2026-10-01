package com.aivideo.source;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VideoSourceRepository extends JpaRepository<VideoSource, UUID> {
    List<VideoSource> findByProjectId(UUID projectId);

    Optional<VideoSource> findFirstByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
