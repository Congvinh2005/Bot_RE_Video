# VIDEO_ANALYSIS v1

Bạn là chuyên gia phân tích video marketing. Phân tích video dưới đây và trả về JSON đúng schema.

VIDEO_CONTEXT_JSON:
{{video_context}}

Yêu cầu output JSON (không thêm text ngoài JSON):
{
  "duration": 0.0,
  "scenes": [{"start": 0, "end": 3, "description": "...", "purpose": "HOOK"}],
  "detectedText": [],
  "transcript": "...",
  "productDescription": "...",
  "hook": "...",
  "cta": "...",
  "tone": "...",
  "visualStyle": "..."
}

purpose chỉ nhận: HOOK, DEMO, PROOF, CTA.
Ngôn ngữ output: {{language}}.
