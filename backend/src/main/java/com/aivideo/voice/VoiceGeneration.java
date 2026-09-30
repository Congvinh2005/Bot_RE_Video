package com.aivideo.voice;

import com.aivideo.content.ContentGeneration;
import com.aivideo.project.Project;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "voice_generations", indexes = {
        @Index(name = "idx_voice_project", columnList = "project_id"),
        @Index(name = "idx_voice_content", columnList = "content_generation_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoiceGeneration {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_generation_id")
    private ContentGeneration contentGeneration;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String voice = "ADAM";

    @Builder.Default
    private Double speed = 1.0;

    @Builder.Default
    private Double pitch = 0.0;

    @Column(length = 50)
    @Builder.Default
    private String emotion = "natural";

    @Column(length = 10)
    @Builder.Default
    private String language = "vi";

    @Column(length = 10)
    @Builder.Default
    private String format = "mp3";

    @Column(name = "storage_key", length = 1024)
    private String storageKey;

    @Column(length = 20)
    @Builder.Default
    private String status = "PENDING";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
