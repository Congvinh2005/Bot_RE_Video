package com.aivideo;

import com.aivideo.analysis.VideoAnalysis;
import com.aivideo.analysis.VideoAnalysisRepository;
import com.aivideo.content.ContentGeneration;
import com.aivideo.content.ContentGenerationRepository;
import com.aivideo.job.JobStatus;
import com.aivideo.job.JobStep;
import com.aivideo.job.VideoJob;
import com.aivideo.job.VideoJobRepository;
import com.aivideo.project.Project;
import com.aivideo.project.ProjectRepository;
import com.aivideo.project.ProjectStatus;
import com.aivideo.project.WorkflowType;
import com.aivideo.source.SourceStatus;
import com.aivideo.source.SourceType;
import com.aivideo.source.VideoSource;
import com.aivideo.source.VideoSourceRepository;
import com.aivideo.user.Role;
import com.aivideo.user.User;
import com.aivideo.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Runs against LOCAL PostgreSQL (no Docker needed).
// Run: DB_URL=jdbc:postgresql://localhost:5432/aivideo DB_USER=aivideo DB_PASSWORD=aivideo mvn test -Dtest=LocalPostgresSmokeTest
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/aivideo}",
        "spring.datasource.username=${DB_USER:aivideo}",
        "spring.datasource.password=${DB_PASSWORD:aivideo}"
})
class LocalPostgresSmokeTest {

    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    VideoSourceRepository videoSourceRepository;
    @Autowired
    VideoAnalysisRepository videoAnalysisRepository;
    @Autowired
    ContentGenerationRepository contentGenerationRepository;
    @Autowired
    VideoJobRepository videoJobRepository;

    @Test
    void flywayMigratedAndGraphPersists() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename",
                String.class);
        assertThat(tables).contains(
                "users", "projects", "video_sources", "products",
                "video_analyses", "content_generations", "voice_generations",
                "video_jobs", "media_assets", "generated_videos", "export_tasks");

        User user = userRepository.save(User.builder()
                .email("local@example.com").passwordHash("h").role(Role.USER).build());
        Project project = projectRepository.save(Project.builder()
                .user(user).name("Local project")
                .workflowType(WorkflowType.NORMAL_VIDEO).status(ProjectStatus.CREATED).build());
        VideoSource source = videoSourceRepository.save(VideoSource.builder()
                .project(project).sourceType(SourceType.UPLOAD)
                .originalFilename("p.mp4").storageKey("video/raw/local-p.mp4")
                .duration(10.0).width(1080).height(1920)
                .status(SourceStatus.READY).build());
        videoAnalysisRepository.save(VideoAnalysis.builder()
                .project(project).videoSource(source)
                .duration(10.0).result(Map.of("hook", "hi")).build());
        contentGenerationRepository.save(ContentGeneration.builder()
                .project(project).version(1).hook("h")
                .hashtags(List.of("#x")).scenes(List.of(Map.of("start", 0)))
                .result(Map.of("hook", "h")).build());
        videoJobRepository.save(VideoJob.builder()
                .project(project).status(JobStatus.PENDING)
                .progress(0).currentStep(JobStep.ANALYZE).build());

        assertThat(projectRepository.findById(project.getId())).isPresent();
        assertThat(videoSourceRepository.findByProjectId(project.getId())).hasSize(1);
    }
}
