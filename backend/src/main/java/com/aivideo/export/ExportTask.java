package com.aivideo.export;

import com.aivideo.project.Project;
import com.aivideo.video.GeneratedVideo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "export_tasks", indexes = {
        @Index(name = "idx_export_project", columnList = "project_id"),
        @Index(name = "idx_export_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportTask {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_video_id")
    private GeneratedVideo generatedVideo;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Builder.Default
    private Integer progress = 0;

    @Column(name = "download_url", columnDefinition = "TEXT")
    private String downloadUrl;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
