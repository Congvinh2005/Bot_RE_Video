package com.aivideo.source;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VideoSourceRepository extends JpaRepository<VideoSource, UUID> {
    List<VideoSource> findByProjectId(UUID projectId);
}
