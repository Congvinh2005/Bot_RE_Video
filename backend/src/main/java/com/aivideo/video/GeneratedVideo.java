package com.aivideo.video;

import com.aivideo.job.VideoJob;
import com.aivideo.project.Project;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "generated_videos", indexes = {
        @Index(name = "idx_genvideo_project", columnList = "project_id"),
        @Index(name = "idx_genvideo_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratedVideo {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_job_id")
    private VideoJob videoJob;

    @Column(name = "storage_key", nullable = false, length = 1024)
    private String storageKey;

    @Column(name = "thumbnail_key", length = 1024)
    private String thumbnailKey;

    private Double duration;

    @Builder.Default
    private Integer width = 1080;

    @Builder.Default
    private Integer height = 1920;

    @Column(length = 20)
    @Builder.Default
    private String status = "PENDING";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
