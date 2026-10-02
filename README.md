# AI Video Content Generator

**AI repurpose engine**: phân tích video được phép sử dụng + product context +
user context → tạo phiên bản marketing mới (script, giọng ADAM, subtitle,
video dọc 1080x1920 chuẩn TikTok).

2 luồng: **A** — TikTok URL → metadata (oEmbed công khai) → user upload source →
pipeline; **B** — upload video + context → pipeline.

## Chạy nhanh

```bash
cp .env.example .env   # điền JWT_SECRET, AI_API_KEY, ELEVENLABS_API_KEY
docker compose up --build -d   # FE :3000, BE :8080, MinIO :9001
```

Dev local không Docker: xem `docs/local-development.md`.

## API tóm tắt

`POST /api/auth/register|login` → `POST /api/projects` → `POST {id}/tiktok|upload|product`
→ `POST {id}/analyze|generate-content|generate-voice|generate-subtitle|generate-video|generate-full`
→ `GET {id}/status|status/stream|result`. Chi tiết: `docs/api.md`.

## Cần key gì

| Key | Dùng cho | Không có thì sao |
|---|---|---|
| `AI_API_KEY` (OpenAI-compatible) | phân tích video, sinh content | task AI báo lỗi rõ, còn lại chạy |
| `ELEVENLABS_API_KEY` (giọng Adam) | voice ADAM (model `eleven_v3` cho tiếng Việt) | như trên |
| `JWT_SECRET` (≥32 ký tự) | đăng nhập | bắt buộc để boot |

## Test

```bash
cd backend && mvn test -Dtest='!DatabaseMigrationTest'  # 166 tests, cần PG + MinIO local
cd ../frontend && npm test && npm run build
```

## Known limitations

- Testcontainers không chạy được với Docker Desktop 4.79 ở máy dev (docker-java lỗi 400)
  → `DatabaseMigrationTest` dành cho CI; local dùng `LocalPostgresSmokeTest` + MinIO thật.
- STT đang Noop (transcript rỗng); muốn transcript thật dùng ElevenLabs Scribe (cùng key TTS).
- Job chạy in-process `@Async` (chưa Redis queue/worker riêng), chưa retry/cancel, chưa rate limit.
- Voice khác ADAM sẽ fallback về ADAM kèm warn log; `speed/pitch` lưu DB nhưng ElevenLabs
  chỉ nhận stability/similarity.
- `voice-id` Adam mặc định theo Voice Library công khai — kiểm tra lại nếu đổi voice.

## Docs

`docs/architecture.md`, `docs/plan.md` (tiến độ 18 task), `docs/database.md`,
`docs/api.md`, `docs/video-pipeline.md`, `docs/ai-pipeline.md`,
`docs/local-development.md`, `docs/deployment.md`.
