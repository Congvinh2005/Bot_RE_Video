package com.aivideo.project;

import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.project.dto.CreateProjectRequest;
import com.aivideo.project.dto.ProjectResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    ProjectRepository projectRepository;

    @InjectMocks
    ProjectService projectService;

    @Test
    void createProjectSavesWithCreatedStatus() {
        when(projectRepository.save(any(Project.class)))
                .thenAnswer(inv -> {
                    Project p = inv.getArgument(0);
                    p.setId(UUID.randomUUID());
                    return p;
                });

        ProjectResponse response = projectService.createProject(
                new CreateProjectRequest("My video", WorkflowType.NORMAL_VIDEO));

        assertThat(response.name()).isEqualTo("My video");
        assertThat(response.workflowType()).isEqualTo(WorkflowType.NORMAL_VIDEO);
        assertThat(response.status()).isEqualTo(ProjectStatus.CREATED);
        assertThat(response.id()).isNotNull();
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void listProjectsReturnsMappedResponses() {
        Project project = Project.builder()
                .name("A").workflowType(WorkflowType.TIKTOK_PRODUCT)
                .status(ProjectStatus.CREATED).build();
        when(projectRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(project));

        List<ProjectResponse> result = projectService.listProjects();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).workflowType()).isEqualTo(WorkflowType.TIKTOK_PRODUCT);
    }

    @Test
    void getProjectThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(projectRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProject(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .satisfies(ex -> assertThat(((ResourceNotFoundException) ex).getCode())
                        .isEqualTo("PROJECT_NOT_FOUND"));
    }

    @Test
    void deleteProjectDeletesWhenExists() {
        UUID id = UUID.randomUUID();
        Project project = Project.builder()
                .name("A").workflowType(WorkflowType.NORMAL_VIDEO)
                .status(ProjectStatus.CREATED).build();
        when(projectRepository.findById(id)).thenReturn(Optional.of(project));

        projectService.deleteProject(id);

        verify(projectRepository).delete(project);
    }

    @Test
    void deleteProjectThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(projectRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.deleteProject(id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(projectRepository, never()).delete(any());
    }
}
