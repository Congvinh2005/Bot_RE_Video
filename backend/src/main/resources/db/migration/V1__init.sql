-- V1: Initial schema for AI Video Content Generator (Task 02)
-- No binary video stored in PostgreSQL, only storage_key -> object storage.

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX idx_users_email ON users(email);

CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    workflow_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_project_user_id ON projects(user_id);
CREATE INDEX idx_project_status ON projects(status);
CREATE INDEX idx_project_created ON projects(created_at);

CREATE TABLE video_sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    source_url TEXT,
    source_type VARCHAR(20) NOT NULL,
    original_filename VARCHAR(512),
    storage_key VARCHAR(1024),
    duration DOUBLE PRECISION,
    width INT,
    height INT,
    fps DOUBLE PRECISION,
    codec VARCHAR(50),
    has_audio BOOLEAN NOT NULL DEFAULT false,
    format VARCHAR(50),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_videosource_project ON video_sources(project_id);
CREATE INDEX idx_videosource_status ON video_sources(status);

CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL UNIQUE REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(255),
    category VARCHAR(128),
    description TEXT,
    price VARCHAR(64),
    features JSONB,
    material VARCHAR(255),
    color VARCHAR(128),
    size VARCHAR(128),
    target_audience TEXT,
    selling_points JSONB,
    product_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX idx_product_project ON products(project_id);

CREATE TABLE video_analyses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    video_source_id UUID REFERENCES video_sources(id) ON DELETE SET NULL,
    duration DOUBLE PRECISION,
    result JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_analysis_project ON video_analyses(project_id);
CREATE INDEX idx_analysis_created ON video_analyses(created_at);

CREATE TABLE content_generations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    version INT NOT NULL DEFAULT 1,
    hook TEXT,
    script TEXT,
    caption TEXT,
    hashtags JSONB,
    cta TEXT,
    scenes JSONB,
    result JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_content_project ON content_generations(project_id);
CREATE INDEX idx_content_project_version ON content_generations(project_id, version);

CREATE TABLE voice_generations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    content_generation_id UUID REFERENCES content_generations(id) ON DELETE SET NULL,
    voice VARCHAR(50) NOT NULL DEFAULT 'ADAM',
    speed DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    pitch DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    emotion VARCHAR(50) NOT NULL DEFAULT 'natural',
    language VARCHAR(10) NOT NULL DEFAULT 'vi',
    format VARCHAR(10) NOT NULL DEFAULT 'mp3',
    storage_key VARCHAR(1024),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_voice_project ON voice_generations(project_id);
CREATE INDEX idx_voice_content ON voice_generations(content_generation_id);

CREATE TABLE video_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    progress INT NOT NULL DEFAULT 0,
    current_step VARCHAR(20),
    error_message TEXT,
    trace_id VARCHAR(64),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_job_project ON video_jobs(project_id);
CREATE INDEX idx_job_status ON video_jobs(status);
CREATE INDEX idx_job_created ON video_jobs(created_at);

CREATE TABLE media_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    asset_type VARCHAR(20) NOT NULL,
    storage_key VARCHAR(1024) NOT NULL UNIQUE,
    filename VARCHAR(512),
    mime_type VARCHAR(128),
    size_bytes BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_media_project ON media_assets(project_id);
CREATE INDEX idx_media_type ON media_assets(asset_type);
CREATE UNIQUE INDEX idx_media_storage_key ON media_assets(storage_key);

CREATE TABLE generated_videos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    video_job_id UUID REFERENCES video_jobs(id) ON DELETE SET NULL,
    storage_key VARCHAR(1024) NOT NULL,
    thumbnail_key VARCHAR(1024),
    duration DOUBLE PRECISION,
    width INT NOT NULL DEFAULT 1080,
    height INT NOT NULL DEFAULT 1920,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_genvideo_project ON generated_videos(project_id);
CREATE INDEX idx_genvideo_status ON generated_videos(status);

CREATE TABLE export_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    generated_video_id UUID REFERENCES generated_videos(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    progress INT NOT NULL DEFAULT 0,
    download_url TEXT,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ
);
CREATE INDEX idx_export_project ON export_tasks(project_id);
CREATE INDEX idx_export_status ON export_tasks(status);
