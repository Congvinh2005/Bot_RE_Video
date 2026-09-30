package com.aivideo;

import com.aivideo.analysis.VideoAnalysis;
import com.aivideo.analysis.VideoAnalysisRepository;
import com.aivideo.content.ContentGeneration;
import com.aivideo.content.ContentGenerationRepository;
import com.aivideo.export.ExportTask;
import com.aivideo.export.ExportTaskRepository;
import com.aivideo.job.JobStatus;
import com.aivideo.job.JobStep;
import com.aivideo.job.VideoJob;
import com.aivideo.job.VideoJobRepository;
import com.aivideo.media.AssetType;
import com.aivideo.media.MediaAsset;
import com.aivideo.media.MediaAssetRepository;
import com.aivideo.product.Product;
import com.aivideo.product.ProductRepository;
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
import com.aivideo.video.GeneratedVideo;
import com.aivideo.video.GeneratedVideoRepository;
import com.aivideo.voice.VoiceGeneration;
import com.aivideo.voice.VoiceGenerationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class DatabaseMigrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    UserRepository userRepository;
    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    VideoSourceRepository videoSourceRepository;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    VideoAnalysisRepository videoAnalysisRepository;
    @Autowired
    ContentGenerationRepository contentGenerationRepository;
    @Autowired
    VoiceGenerationRepository voiceGenerationRepository;
    @Autowired
    VideoJobRepository videoJobRepository;
    @Autowired
    MediaAssetRepository mediaAssetRepository;
    @Autowired
    GeneratedVideoRepository generatedVideoRepository;
    @Autowired
    ExportTaskRepository exportTaskRepository;

    @Test
    void flywayMigratesAllTables() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename",
                String.class);
        assertThat(tables).contains(
                "users", "projects", "video_sources", "products",
                "video_analyses", "content_generations", "voice_generations",
                "video_jobs", "media_assets", "generated_videos", "export_tasks",
                "flyway_schema_history");
    }

    @Test
    void fullProjectGraphPersists() {
        User user = userRepository.save(User.builder()
                .email("test@example.com")
                .passwordHash("hashed")
                .role(Role.USER)
                .build());

        Project project = projectRepository.save(Project.builder()
                .user(user)
                .name("Test project")
                .workflowType(WorkflowType.NORMAL_VIDEO)
                .status(ProjectStatus.CREATED)
                .build());

        VideoSource source = videoSourceRepository.save(VideoSource.builder()
                .project(project)
                .sourceType(SourceType.UPLOAD)
                .originalFilename("product.mp4")
                .storageKey("video/raw/product.mp4")
                .duration(15.2)
                .width(1080)
                .height(1920)
                .fps(30.0)
                .codec("h264")
                .hasAudio(true)
                .format("mp4")
                .status(SourceStatus.READY)
                .build());

        productRepository.save(Product.builder()
                .project(project)
                .name("Bo ngu nu 2 day")
                .category("Fashion")
                .description("Thun mem mat")
                .price("199000 VND")
                .features(List.of("mem", "mat"))
                .material("thun")
                .color("den")
                .size("M")
                .targetAudience("nu 18-30")
                .sellingPoints(List.of("mat", "re"))
                .productUrl("https://shop.example/p/1")
                .build());

        videoAnalysisRepository.save(VideoAnalysis.builder()
                .project(project)
                .videoSource(source)
                .duration(15.2)
                .result(Map.of("hook", "hello", "tone", "natural"))
                .build());

        ContentGeneration content = contentGenerationRepository.save(ContentGeneration.builder()
                .project(project)
                .version(1)
                .hook("hook")
                .script("script")
                .caption("caption")
                .hashtags(List.of("#fashion"))
                .cta("Mua ngay")
                .scenes(List.of(Map.of("start", 0, "end", 3, "purpose", "HOOK")))
                .result(Map.of("hook", "hook"))
                .build());

        voiceGenerationRepository.save(VoiceGeneration.builder()
                .project(project)
                .contentGeneration(content)
                .voice("ADAM")
                .storageKey("audio/voice.mp3")
                .status("COMPLETED")
                .build());

        VideoJob job = videoJobRepository.save(VideoJob.builder()
                .project(project)
                .status(JobStatus.PROCESSING)
                .progress(40)
                .currentStep(JobStep.VOICE)
                .traceId("trace-1")
                .build());

        mediaAssetRepository.save(MediaAsset.builder()
                .project(project)
                .assetType(AssetType.VIDEO)
                .storageKey("video/raw/product.mp4")
                .filename("product.mp4")
                .mimeType("video/mp4")
                .sizeBytes(1024L)
                .build());

        GeneratedVideo video = generatedVideoRepository.save(GeneratedVideo.builder()
                .project(project)
                .videoJob(job)
                .storageKey("video/final/out.mp4")
                .duration(15.2)
                .status("COMPLETED")
                .build());

        exportTaskRepository.save(ExportTask.builder()
                .project(project)
                .generatedVideo(video)
                .status("COMPLETED")
                .progress(100)
                .build());

        assertThat(projectRepository.findById(project.getId())).isPresent();
        assertThat(videoSourceRepository.findByProjectId(project.getId())).hasSize(1);
        assertThat(productRepository.findByProjectId(project.getId())).isPresent();
        assertThat(contentGenerationRepository.findByProjectIdOrderByVersionDesc(project.getId())).hasSize(1);
        assertThat(videoJobRepository.findByProjectIdOrderByCreatedAtDesc(project.getId())).hasSize(1);
    }
}
