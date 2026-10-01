package com.aivideo.project.dto;

import com.aivideo.project.WorkflowType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank(message = "name must not be blank")
        @Size(max = 255, message = "name must be at most 255 characters")
        String name,

        @NotNull(message = "workflowType must not be null")
        WorkflowType workflowType
) {
}
