# Local Development

## Yêu cầu

Java 21, Maven, Node 20+, `ffmpeg` + `ffprobe` trong PATH (khuyên `ffmpeg-full`
để có burn subtitle + overlay: `brew install ffmpeg-full`),
PostgreSQL 14+, MinIO chạy ở `localhost:9000`.

## Chạy nhanh (không Docker)

```bash
# 1. Postgres + MinIO local
psql -d postgres -c "CREATE DATABASE aivideo OWNER aivideo;"
minio server /tmp/minio-data --address ":9000"   # user/pass: minioadmin

# 2. Backend (tự Flyway migrate + validate schema)
cd backend
DB_URL=jdbc:postgresql://localhost:5432/aivideo DB_USER=aivideo DB_PASSWORD=aivideo \
  mvn spring-boot:run
# -> http://localhost:8080

# 3. Frontend (proxy /api -> :8080)
cd frontend && npm install && npm run dev
# -> http://localhost:3000
```

Env dev tiện dùng: `FFMPEG_PATH=/opt/homebrew/opt/ffmpeg-full/bin/ffmpeg`,
`FFPROBE_PATH=.../ffprobe`, `AI_API_KEY`, `ELEVENLABS_API_KEY`, `JWT_SECRET`.

## Test

```bash
cd backend
mvn test -Dtest='!DatabaseMigrationTest'   # full suite, cần PG + MinIO local
# DatabaseMigrationTest cần Testcontainers (Docker Engine tương thích).
# Hiện Docker Desktop 4.79 ở máy dev không chạy được docker-java -> bỏ qua local,
# chạy ở CI.

cd ../frontend && npm test   # tsc --noEmit
npm run build
```

## Quy ước commit

`<type>(<scope>): <mô tả tiếng Việt>` — feat, fix, docs, style, refactor, test, chore.
Chia nhỏ commit, working tree sạch sau mỗi task, tick `[X]` trong `docs/plan.md`.
