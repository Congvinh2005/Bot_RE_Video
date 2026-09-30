# PROJECT: AI VIDEO CONTENT GENERATOR

## 1. Mục tiêu

Xây dựng một web application cho phép người dùng tạo video marketing/social-commerce bằng AI từ:

### Workflow A — TikTok Product Video

Input:

* TikTok video URL
* Video phải là video người dùng có quyền sử dụng/biến đổi
* Video có thể chứa TikTok Shop/product information
* Người dùng có thể nhập thêm context tùy chọn

Hệ thống cần:

1. Nhận TikTok URL.
2. Xác định và phân tích metadata/video/product information trong phạm vi API hoặc dữ liệu mà hệ thống được phép truy cập.
3. Phân tích nội dung video:

   * Caption
   * Text trên video
   * Scene
   * Object/product
   * Voice/audio/transcript nếu được phép xử lý
   * Video duration
   * Scene structure
   * Hook
   * CTA
4. Phân tích thông tin sản phẩm từ product URL hoặc dữ liệu sản phẩm mà người dùng cung cấp.
5. Kết hợp:

   * Video context
   * Product context
   * User context
6. Dùng AI tạo:

   * Hook mới
   * Caption mới
   * Hashtag
   * Script
   * CTA
   * Video scene plan
7. Tạo một video mới dựa trên asset/video mà người dùng có quyền sử dụng.
8. Cho phép preview.
9. Cho phép chỉnh sửa.
10. Export video.

Không được thiết kế hệ thống để tải/reup trái phép video của bên thứ ba hoặc bypass cơ chế bảo vệ của TikTok.

---

# 2. Workflow B — Normal Video + Context

Input:

* User upload một video bình thường.
* User nhập context.

Ví dụ:

Video:
product.mp4

Context:

"Bán bộ ngủ nữ 2 dây, chất liệu thun mềm mát, phù hợp mùa hè, muốn content ngắn và tự nhiên."

System:

Video
+
Context
↓
AI Video Analysis
↓
Generate Marketing Script
↓
Generate ADAM Voice
↓
Synchronize Voice
↓
Add Subtitle
↓
Add Text Overlay
↓
Generate Final Video

Output:

* Script
* Voice audio
* Subtitle
* Final video
* Caption
* Hashtags
* CTA

---

# 3. ADAM Voice

Hệ thống phải hỗ trợ voice profile tên:

ADAM

ADAM là voice profile dùng cho voice-over marketing.

Không hard-code provider.

Thiết kế abstraction:

VoiceProvider
↓
AdamVoiceProvider

Interface:

generateSpeech(
String text,
VoiceOptions options
)

VoiceOptions:

* voiceId
* speed
* pitch
* emotion
* language
* format

Cho phép thay đổi TTS provider trong tương lai.

Nếu "ADAM" là giọng của một người thật hoặc voice clone, chỉ sử dụng khi có quyền/giấy phép phù hợp.

---

# 4. Tech Stack

Backend:

* Java 21
* Spring Boot
* Spring Security
* REST API
* JPA/Hibernate
* PostgreSQL
* Redis
* Maven
* Lombok nếu phù hợp, nhưng không phụ thuộc quá mức vào Lombok.

Frontend:

* React hoặc Angular
* TypeScript
* Tailwind CSS
* Video preview
* Upload progress
* Job progress

Media:

* FFmpeg
* FFprobe

AI:

* LLM provider abstraction
* Vision/model abstraction
* Speech-to-text abstraction
* Text-to-speech abstraction

Storage:

* S3-compatible storage
* MinIO cho local development

Background processing:

* Redis Queue hoặc Spring-based async job architecture.

Docker:

* Docker
* Docker Compose

---

# 5. Architecture

Thiết kế theo modular architecture.

Backend:

com.aivideo
├── auth
├── user
├── project
├── source
├── product
├── analysis
├── content
├── voice
├── video
├── media
├── job
├── export
├── common
└── config

Không viết toàn bộ business logic vào Controller.

Controller
↓
Service
↓
Domain / Repository
↓
Infrastructure

---

# 6. Core Domain

Các entity chính:

User

Project

VideoSource

Product

VideoAnalysis

ContentGeneration

VoiceGeneration

VideoJob

MediaAsset

GeneratedVideo

ExportTask

---

# 7. Project

Một Project đại diện cho một lần tạo content.

Ví dụ:

Project
{
id,
name,
workflowType,
status,
createdAt,
updatedAt
}

workflowType:

TIKTOK_PRODUCT
NORMAL_VIDEO

---

# 8. VideoSource

Lưu:

* source URL
* source type
* original filename
* duration
* width
* height
* fps
* status
* storage key

Không lưu raw video trực tiếp vào PostgreSQL.

Video phải nằm trong object storage.

---

# 9. Video Analysis

AI cần tạo structured result.

Ví dụ:

{
"duration": 15.2,
"scenes": [
{
"start": 0,
"end": 3,
"description": "...",
"purpose": "HOOK"
}
],
"detectedText": [],
"transcript": "...",
"productDescription": "...",
"hook": "...",
"cta": "...",
"tone": "...",
"visualStyle": "..."
}

Lưu analysis result dưới dạng JSONB nếu phù hợp.

---

# 10. Content Generation

AI phải tạo structured output.

Ví dụ:

{
"hook": "...",
"script": "...",
"caption": "...",
"hashtags": [],
"cta": "...",
"scenes": [
{
"start": 0,
"end": 3,
"voiceText": "...",
"overlayText": "..."
}
]
}

Không parse AI response bằng các string hack không ổn định.

Ưu tiên structured JSON response + schema validation.

---

# 11. Video Generation Pipeline

Pipeline:

INPUT
↓
Validate
↓
Download / Upload Asset
↓
FFprobe
↓
Scene Analysis
↓
Transcript
↓
AI Analysis
↓
Content Generation
↓
Voice Generation
↓
Subtitle Generation
↓
Video Composition
↓
FFmpeg
↓
Quality Check
↓
Object Storage
↓
READY

Mỗi bước phải có job status.

---

# 12. Job State

Job:

PENDING
PROCESSING
COMPLETED
FAILED
CANCELLED

Có:

progress

0 → 100

currentStep

errorMessage

startedAt

completedAt

---

# 13. API

Thiết kế REST API.

Ví dụ:

POST /api/projects

POST /api/projects/{id}/tiktok

POST /api/projects/{id}/upload

POST /api/projects/{id}/analyze

POST /api/projects/{id}/generate-content

POST /api/projects/{id}/generate-voice

POST /api/projects/{id}/generate-video

GET /api/projects/{id}

GET /api/projects/{id}/status

GET /api/projects/{id}/result

DELETE /api/projects/{id}

---

# 14. TikTok Workflow

Endpoint:

POST /api/projects/{id}/tiktok

Request:

{
"url": "...",
"context": "..."
}

Backend:

1. Validate URL.
2. Resolve permitted metadata.
3. Obtain accessible source/product information.
4. Analyze metadata.
5. Create VideoSource.
6. Create Product context.
7. Start analysis job.

Nếu TikTok không cung cấp dữ liệu cần thiết qua API hợp lệ:

Không bypass protection.

Trả về trạng thái yêu cầu:

"USER_UPLOAD_REQUIRED"

Frontend hiển thị:

"Không thể truy cập video trực tiếp. Hãy upload video mà bạn có quyền sử dụng."

---

# 15. Product Context

Product information có thể đến từ:

* API chính thức nếu có quyền truy cập
* URL được người dùng cung cấp
* User input
* Metadata
* AI extraction từ dữ liệu được cung cấp

Không phụ thuộc vào scraping trái phép.

Product model:

{
name,
category,
description,
price,
features,
materials,
colors,
sizes,
targetAudience,
sellingPoints,
productUrl
}

---

# 16. Context Engine

Tạo một Context Engine.

Input:

SOURCE_CONTEXT
PRODUCT_CONTEXT
USER_CONTEXT
VIDEO_ANALYSIS

↓

UnifiedContext

Ví dụ:

{
"source": {},
"product": {},
"userContext": {},
"video": {}
}

AI chỉ nhận UnifiedContext.

Mục tiêu:

Không để mỗi service tự tạo prompt riêng biệt.

---

# 17. Prompt Engine

Không hard-code prompt trong Controller.

Tạo:

PromptTemplate

PromptVersion

PromptRenderer

PromptType:

VIDEO_ANALYSIS
CONTENT_GENERATION
VOICE_SCRIPT
SCENE_GENERATION
CAPTION_GENERATION

Cho phép thay đổi prompt version.

---

# 18. Voice Pipeline

Script:

AI generated script
↓
Voice Provider
↓
ADAM
↓
audio.wav
↓
Audio normalization
↓
Video synchronization

Voice options:

{
"voice": "ADAM",
"speed": 1.0,
"pitch": 0,
"emotion": "natural"
}

---

# 19. Subtitle

Generate:

SRT

hoặc

ASS

Ví dụ:

00:00:00,000 --> 00:00:02,500

"Bộ ngủ mềm mát cho nàng"

Subtitle phải đồng bộ với voice.

---

# 20. Video Composition

FFmpeg pipeline có thể:

* Trim
* Crop
* Resize
* Overlay
* Subtitle
* Add voice
* Normalize audio
* Add background music nếu người dùng có quyền sử dụng
* Add CTA
* Add watermark nếu user bật
* Export MP4

Default:

1080x1920

vertical video

H.264

AAC

---

# 21. Frontend

Dashboard:

Projects

Create Project

Templates

Voice

Generated Videos

---

Create Project có 2 lựa chọn:

[ TikTok Product Video ]

[ Normal Video ]

---

TikTok Product Video:

TikTok URL
Product URL (optional)
Context (optional)

Button:

Analyze

---

Normal Video:

Upload video

Context textarea

Voice:

ADAM

Button:

Generate

---

# 22. Progress UI

Hiển thị:

Uploading
✓

Analyzing video
✓

Generating content
✓

Generating ADAM voice
...

Composing video
...

Finalizing
...

Progress bar.

---

# 23. Result Page

Hiển thị:

Video preview

Script

Caption

Hashtags

CTA

Product link

Download

Regenerate

Edit Content

Regenerate Voice

Regenerate Video

---

# 24. Error Handling

Không được để exception raw trả về frontend.

Chuẩn hóa:

{
"code": "VIDEO_ANALYSIS_FAILED",
"message": "Unable to analyze video",
"details": null,
"traceId": "..."
}

Các lỗi cần xử lý:

INVALID_URL

UNSUPPORTED_SOURCE

SOURCE_ACCESS_DENIED

UPLOAD_FAILED

VIDEO_FORMAT_INVALID

AI_PROVIDER_ERROR

TTS_PROVIDER_ERROR

FFMPEG_ERROR

STORAGE_ERROR

TIMEOUT

---

# 25. Security

Implement:

JWT authentication

Role:

USER
ADMIN

Validate:

* File type
* File size
* URL
* Filename
* MIME type

Không cho phép arbitrary FFmpeg command từ frontend.

Không expose storage credentials.

API key phải nằm trong environment variables.

---

# 26. Observability

Mỗi generation phải có:

traceId

projectId

jobId

Các log quan trọng:

START_JOB
VIDEO_ANALYSIS_STARTED
AI_GENERATION_STARTED
TTS_STARTED
FFMPEG_STARTED
JOB_COMPLETED
JOB_FAILED

Không log:

* API keys
* JWT
* password
* sensitive user data

---

# 27. Docker

docker-compose:

postgres
redis
minio
backend
frontend

Optional:

worker

---

# 28. Testing

Backend:

JUnit

Mockito

Spring Boot Test

Test:

* Project creation
* TikTok URL validation
* Upload
* Content generation
* TTS service
* Job status
* Error handling

Integration test:

PostgreSQL
Redis
MinIO

---

# 29. Documentation

Phải tạo:

README.md

docs/
├── architecture.md
├── database.md
├── api.md
├── video-pipeline.md
├── ai-pipeline.md
├── local-development.md
└── deployment.md

Có Mermaid diagrams:

architecture
database ERD
video pipeline
sequence diagrams

---

# 30. Development Rules

Không viết tất cả code một lần.

Chia thành các task.

Mỗi task phải:

1. Có mục tiêu.
2. Có files cần tạo/sửa.
3. Implement.
4. Test.
5. Verify.
6. Update documentation.

Không phá code đang chạy.

Không xóa code không liên quan.

Không thay đổi architecture giữa chừng nếu chưa giải thích lý do.

Nếu thiếu thông tin:

Đưa ra assumption rõ ràng.

Không tự tạo API key.

Không giả định một external API tồn tại.

---

# 31. Definition of Done

Một task chỉ DONE khi:

* Code compile.
* Tests pass.
* API hoạt động.
* Error handling có.
* Documentation cập nhật.
* Không có TODO quan trọng bị bỏ lại.

Sau mỗi task:

Report:

IMPLEMENTED
FILES_CHANGED
TESTS
HOW_TO_RUN
KNOWN_LIMITATIONS
NEXT_TASK

Bắt đầu bằng việc phân tích repository hiện tại trước khi viết code.
Không tự ý rewrite toàn bộ project.
