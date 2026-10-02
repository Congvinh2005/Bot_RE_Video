# Video Pipeline

```mermaid
flowchart TB
    IN[Upload mp4/mov/webm\nTikTok URL + upload tay] --> VAL[Validate ext + MIME + size]
    VAL --> S3[MinIO video/ + VideoSource]
    S3 --> FP[FFprobe: duration W H fps codec audio]
    FP --> SC[Scene detect: ffmpeg select gt scene]
    SC --> FR[Frame extract JPEG giữa scene max 5]
    FR --> AN[AI Analysis -> VideoAnalysis JSONB]
    AN --> CG[AI Content hook script caption scenes v2]
    CG --> TTS[ElevenLabs ADAM mp3 -> MinIO audio/]
    TTS --> SUB[SRT chia cue theo duration audio -> MinIO subtitle/]
    SUB --> COMP[FFmpeg: scale/crop 1080x1920 H264 + voice loudnorm + burn sub + drawtext overlay + cap 30s]
    COMP --> OUT[MP4 + thumbnail -> MinIO -> GeneratedVideo]
    OUT --> READY[READY + ExportTask downloadUrl]
```

## Chuẩn output (TikTok 9:16)

- 1080x1920, H.264 (`yuv420p`, CRF 23, 30fps) + AAC 128k/44.1kHz stereo, `.mp4`.
- Voice thay hoàn toàn audio gốc (`-map 1:a`), chuẩn hóa `loudnorm`.
- Source ngắn hơn voice thì loop (`-stream_loop`); dài hơn voice thì `-shortest`; cứng tối đa 30s.
- Subtitle burn bằng libass; overlay text từng scene bằng drawtext (escape `%` -> `\\%`).
- Yêu cầu ffmpeg **full** (libass + freetype) + font chữ (`VIDEO_FONT_PATH`).

## Sequence generate-video

```mermaid
sequenceDiagram
    participant FE as Frontend
    participant API as VideoController
    participant VS as VideoService
    participant S3 as MinIO
    participant FF as FFmpeg
    FE->>API: POST /generate-video {contentId, voiceId}
    API->>VS: generateVideo()
    VS->>S3: download source + voice
    VS->>FF: probe voice duration
    VS->>VS: render SRT temp
    VS->>FF: compose() scale/crop/mix/burn/overlay
    VS->>FF: probe output + extract thumbnail
    VS->>S3: upload final.mp4 + thumb.jpg
    VS-->>FE: 201 {videoUrl, thumbnailUrl, 1080x1920}
```
