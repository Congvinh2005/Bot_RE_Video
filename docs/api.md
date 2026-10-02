# API Reference

Base URL: `/api`. Auth: `Authorization: Bearer <jwt>` cho mọi endpoint trừ `/api/auth/**`.
Lỗi chuẩn: `{code, message, details, traceId}` + header `X-Trace-Id`.

## Auth

| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/api/auth/register` | `{email, password(≥6)}` | `{token, email, role}` |
| POST | `/api/auth/login` | `{email, password}` | `{token, email, role}` |

## Projects

| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/api/projects` | `{name, workflowType: TIKTOK_PRODUCT\|NORMAL_VIDEO}` | 201 Project |
| GET | `/api/projects` | | Project[] |
| GET | `/api/projects/{id}` | | Project |
| DELETE | `/api/projects/{id}` | | 204 |

## Source & Product

| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/api/projects/{id}/upload` | multipart `file` (mp4/mov/webm, ≤MAX_UPLOAD_MB) | 201 VideoSource + FFprobe metadata |
| POST | `/api/projects/{id}/tiktok` | `{url, context?}` | 200 `{status: USER_UPLOAD_REQUIRED, message, metadata?, videoSource}` |
| POST | `/api/projects/{id}/product` | `{productUrl?, product?}` (upsert) | 200 Product |

## AI Pipeline

| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/api/projects/{id}/analyze` | `{videoSourceId?}` (mặc định source mới nhất) | 202 `{jobId, status}` |
| POST | `/api/projects/{id}/generate-content` | `{userContext?}` | 201 Content (versioned) |
| PUT | `/api/projects/{id}/content` | `{hook?, script?, caption?, hashtags?, cta?}` (tạo version mới) | 200 Content |
| POST | `/api/projects/{id}/generate-voice` | `{contentGenerationId, voice?, speed?, pitch?, emotion?}` | 201 Voice + `audioUrl` |
| POST | `/api/projects/{id}/generate-subtitle` | `{voiceGenerationId}` | 201 `{storageKey, subtitleUrl, cueCount, audioDurationSeconds}` |
| POST | `/api/projects/{id}/generate-video` | `{contentGenerationId, voiceGenerationId}` | 201 Video 1080x1920 + urls |
| POST | `/api/projects/{id}/generate-full` | `{userContext?}` (full pipeline) | 202 `{jobId, status}` |

## Job & Result

| Method | Path | Response |
|---|---|---|
| GET | `/api/projects/{id}/status` | `{jobId, status, progress, currentStep, errorMessage}` (404 nếu chưa chạy) |
| GET | `/api/projects/{id}/status/stream` | SSE `text/event-stream`, 1 event/s tới khi job terminal |
| GET | `/api/projects/{id}/result` | `{content, voice, video, product}` mới nhất (kèm presigned URL) |

## Mã lỗi chính

`PROJECT_NOT_FOUND, CONTENT_NOT_FOUND, VOICE_NOT_FOUND, SOURCE_NOT_FOUND, JOB_NOT_FOUND`
`VALIDATION_ERROR, INVALID_REQUEST, INVALID_URL, UNSUPPORTED_SOURCE`
`VIDEO_FORMAT_INVALID, FILE_TOO_LARGE, UPLOAD_FAILED, FFMPEG_ERROR`
`AI_PROVIDER_ERROR (502), TTS_PROVIDER_ERROR (502), STORAGE_ERROR`
`PRODUCT_INPUT_REQUIRED, VOICE_NOT_READY, CONTENT_EMPTY, VOICE_INVALID`
`EMAIL_ALREADY_EXISTS, INVALID_CREDENTIALS, INVALID_TOKEN, AUTH_REQUIRED (401)`
