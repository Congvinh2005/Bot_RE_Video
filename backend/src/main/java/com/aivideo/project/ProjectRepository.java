package com.aivideo.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByUserId(UUID userId);
    List<Project> findByStatus(ProjectStatus status);
    List<Project> findAllByOrderByCreatedAtDesc();
}
