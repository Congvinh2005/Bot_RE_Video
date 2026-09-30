package com.aivideo.job;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VideoJobRepository extends JpaRepository<VideoJob, UUID> {
    List<VideoJob> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
    List<VideoJob> findByStatus(JobStatus status);
}
