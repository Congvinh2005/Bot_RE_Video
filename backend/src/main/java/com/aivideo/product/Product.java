package com.aivideo.product;

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
@Table(name = "products", indexes = {
        @Index(name = "idx_product_project", columnList = "project_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private Project project;

    @Column(length = 255)
    private String name;

    @Column(length = 128)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 64)
    private String price;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> features;

    @Column(length = 255)
    private String material;

    @Column(length = 128)
    private String color;

    @Column(length = 128)
    private String size;

    @Column(name = "target_audience", columnDefinition = "TEXT")
    private String targetAudience;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selling_points", columnDefinition = "jsonb")
    private List<String> sellingPoints;

    @Column(name = "product_url", columnDefinition = "TEXT")
    private String productUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
