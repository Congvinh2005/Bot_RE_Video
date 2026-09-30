package com.aivideo.video;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GeneratedVideoRepository extends JpaRepository<GeneratedVideo, UUID> {
    List<GeneratedVideo> findByProjectId(UUID projectId);
}
