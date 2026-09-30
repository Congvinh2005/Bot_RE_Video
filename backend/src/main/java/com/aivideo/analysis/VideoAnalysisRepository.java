package com.aivideo.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VideoAnalysisRepository extends JpaRepository<VideoAnalysis, UUID> {
    List<VideoAnalysis> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
