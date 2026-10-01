package com.aivideo.ai;

import java.util.Map;

/** Abstraction LLM. Key và model lấy từ env, không hard-code provider. */
public interface AiProvider {

    String generateText(String prompt);

    /** Output JSON validate theo schema (responseType). Lỗi parse -> AiException. */
    <T> T generateStructured(String prompt, Class<T> responseType);

    String analyzeImage(String prompt, byte[] image, String mimeType);

    String analyzeVideoContext(String prompt, Map<String, Object> videoContext);

    /** Video context -> structured JSON validate theo schema. */
    default <T> T analyzeVideoContextStructured(String prompt, Map<String, Object> videoContext,
                                                com.fasterxml.jackson.databind.ObjectMapper objectMapper,
                                                Class<T> responseType) {
        String json = analyzeVideoContext(prompt, videoContext);
        try {
            return objectMapper.readValue(json, responseType);
        } catch (Exception e) {
            throw new AiException("AI_PROVIDER_ERROR",
                    "AI returned invalid structured output", e);
        }
    }
}
