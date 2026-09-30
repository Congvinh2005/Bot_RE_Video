# Database — AI Video Content Generator (Task 02)

> Flyway ưu tiên. Không lưu video binary vào PostgreSQL — chỉ lưu `storage_key` → MinIO/S3.

## 1. Tables & Relationships

- `users (1) — (N) projects`: `projects.user_id → users.id ON DELETE SET NULL`
- `projects (1) — (N) video_sources | video_analyses | content_generations | voice_generations | video_jobs | generated_videos | export_tasks | media_assets`: FK `project_id ON DELETE CASCADE`
- `products.project_id UNIQUE`: 1 project — 1 product (`OneToOne`)
- `video_analyses.video_source_id → video_sources.id ON DELETE SET NULL`
- `voice_generations.content_generation_id → content_generations.id ON DELETE SET NULL`
- `generated_videos.video_job_id → video_jobs.id ON DELETE SET NULL`
- `export_tasks.generated_video_id → generated_videos.id ON DELETE SET NULL`

JSONB: `products.features/selling_points`, `video_analyses.result`, `content_generations.hashtags/scenes/result`.

Enums (VARCHAR, không dùng PG enum để dễ migrate):
`Role USER|ADMIN`, `WorkflowType TIKTOK_PRODUCT|NORMAL_VIDEO`, `ProjectStatus CREATED|PROCESSING|COMPLETED|FAILED`,
`SourceType TIKTOK|UPLOAD|TEMPLATE`, `SourceStatus PENDING|READY|FAILED|USER_UPLOAD_REQUIRED`,
`JobStatus PENDING|PROCESSING|COMPLETED|FAILED|CANCELLED`, `JobStep ANALYZE|CONTENT|VOICE|SUBTITLE|VIDEO|EXPORT`,
`AssetType VIDEO|AUDIO|SUBTITLE|THUMBNAIL`, voice `ADAM` (string mở rộng).

## 2. ERD

```mermaid
erDiagram
    users ||--o{ projects : owns
    projects ||--o{ video_sources : has
    projects ||--o| products : has
    projects ||--o{ video_analyses : has
    projects ||--o{ content_generations : has
    projects ||--o{ voice_generations : has
    projects ||--o{ video_jobs : has
    projects ||--o{ media_assets : has
    projects ||--o{ generated_videos : has
    projects ||--o{ export_tasks : has
    video_sources ||--o{ video_analyses : analyzed
    content_generations ||--o{ voice_generations : voiced
    video_jobs ||--o{ generated_videos : produces
    generated_videos ||--o{ export_tasks : exports
    users {
        uuid id PK
        string email UK
        string password_hash
        string role
    }
    projects {
        uuid id PK
        uuid user_id FK
        string name
        string workflow_type
        string status
    }
    video_sources {
        uuid id PK
        uuid project_id FK
        text source_url
        string source_type
        string storage_key
        float duration
        int width
        int height
    }
    products {
        uuid id PK
        uuid project_id FK_UK
        string name
        jsonb features
        jsonb selling_points
    }
    video_analyses {
        uuid id PK
        uuid project_id FK
        jsonb result
    }
    content_generations {
        uuid id PK
        uuid project_id FK
        int version
        jsonb scenes
        jsonb result
    }
    voice_generations {
        uuid id PK
        uuid project_id FK
        string voice
        string storage_key
    }
    video_jobs {
        uuid id PK
        uuid project_id FK
        string status
        int progress
        string current_step
    }
    media_assets {
        uuid id PK
        uuid project_id FK
        string asset_type
        string storage_key_UK
    }
    generated_videos {
        uuid id PK
        uuid project_id FK
        string storage_key
    }
    export_tasks {
        uuid id PK
        uuid project_id FK
        string status
    }
```

## 3. Indexes

- `users(email)` unique; `projects(user_id, status, created_at)`
- `video_sources(project_id, status)`; `products(project_id)` unique
- `video_analyses(project_id, created_at)`; `content_generations(project_id, [project_id+version])`
- `voice_generations(project_id, content_generation_id)`; `video_jobs(project_id, status, created_at)`
- `media_assets(project_id, asset_type, storage_key unique)`; `generated_videos(project_id, status)`; `export_tasks(project_id, status)`

## 4. Migration

- `backend/src/main/resources/db/migration/V1__init.sql` — full schema, `gen_random_uuid()` PK, FK + index.
- Config: `spring.flyway.enabled=true`, `jpa.hibernate.ddl-auto=validate` (không dùng `update` ở prod).
- Binary: video/audio/subtitle/thumbnail chỉ ở MinIO (`storage_key`), Postgres giữ metadata + JSONB.

## 5. Verify

```bash
workdir backend
mvn compile  # entities + repos compile OK

# Cách 1 — Testcontainers (CI / máy có Docker daemon chạy):
mvn test -Dtest=DatabaseMigrationTest
# Tự dựng postgres:16-alpine, Flyway migrate, assert 11 tables + full graph persist

# Cách 2 — Postgres local (khi Docker daemon tắt, đã verify 30/09/2026, PG14):
psql "postgresql://aivideo:aivideo@localhost:5432/aivideo" -v ON_ERROR_STOP=1 \
  -f src/main/resources/db/migration/V1__init.sql
DB_URL=jdbc:postgresql://localhost:5432/aivideo DB_USER=aivideo DB_PASSWORD=aivideo \
  mvn test -Dtest=LocalPostgresSmokeTest
# Kết quả: Tests run 1, Failures 0, Errors 0; Flyway V1 applied; Hibernate ddl-auto=validate OK; 11 repos wired
```

Assumption: `products` 1-1 với project (đủ cho 2 luồng hiện tại); nếu sau này 1 project nhiều sản phẩm → bỏ unique, thêm `product_id` vào content.
