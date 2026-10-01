package com.aivideo.project;

import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.project.dto.CreateProjectRequest;
import com.aivideo.project.dto.ProjectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        Project project = Project.builder()
                .name(request.name())
                .workflowType(request.workflowType())
                .status(ProjectStatus.CREATED)
                .build();
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjects() {
        return projectRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID id) {
        return ProjectResponse.from(findOrThrow(id));
    }

    @Transactional
    public void deleteProject(UUID id) {
        projectRepository.delete(findOrThrow(id));
    }

    private Project findOrThrow(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + id));
    }
}
