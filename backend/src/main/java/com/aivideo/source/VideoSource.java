package com.aivideo.source;

import com.aivideo.project.Project;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "video_sources", indexes = {
        @Index(name = "idx_videosource_project", columnList = "project_id"),
        @Index(name = "idx_videosource_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoSource {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType;

    @Column(name = "original_filename", length = 512)
    private String originalFilename;

    @Column(name = "storage_key", length = 1024)
    private String storageKey;

    private Double duration;

    private Integer width;

    private Integer height;

    private Double fps;

    @Column(length = 50)
    private String codec;

    @Column(name = "has_audio")
    @Builder.Default
    private Boolean hasAudio = false;

    @Column(length = 50)
    private String format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SourceStatus status = SourceStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
