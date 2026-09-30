package com.aivideo.content;

import com.aivideo.project.Project;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "content_generations", indexes = {
        @Index(name = "idx_content_project", columnList = "project_id"),
        @Index(name = "idx_content_project_version", columnList = "project_id, version")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentGeneration {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(columnDefinition = "TEXT")
    private String hook;

    @Column(name = "script", columnDefinition = "TEXT")
    private String script;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> hashtags;

    @Column(columnDefinition = "TEXT")
    private String cta;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, Object>> scenes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> result;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
