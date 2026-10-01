package com.aivideo.project.dto;

import com.aivideo.project.Project;
import com.aivideo.project.ProjectStatus;
import com.aivideo.project.WorkflowType;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        WorkflowType workflowType,
        ProjectStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getWorkflowType(),
                project.getStatus(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }
}
