package com.aivideo.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Provider đầu tiên theo chuẩn OpenAI chat-completions.
 * Tương thích mọi backend OpenAI-compatible (OpenAI, Azure, Ollama, vLLM...).
 * Thay provider chỉ cần thêm class implement AiProvider.
 */
@Component
@Slf4j
public class OpenAiCompatibleProvider implements AiProvider {

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Autowired
    public OpenAiCompatibleProvider(AiProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, buildClient(properties));
    }

    OpenAiCompatibleProvider(AiProperties properties, ObjectMapper objectMapper, RestClient restClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClient;
    }

    private static RestClient buildClient(AiProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    public String generateText(String prompt) {
        return chat(List.of(Map.of("role", "user", "content", prompt)), false);
    }

    @Override
    public <T> T generateStructured(String prompt, Class<T> responseType) {
        String json = chat(List.of(Map.of("role", "user", "content", prompt)), true);
        try {
            return objectMapper.readValue(json, responseType);
        } catch (Exception e) {
            throw new AiException("AI_PROVIDER_ERROR",
                    "AI returned invalid structured output", e);
        }
    }

    @Override
    public String analyzeImage(String prompt, byte[] image, String mimeType) {
        String dataUrl = "data:" + mimeType + ";base64,"
                + Base64.getEncoder().encodeToString(image);
        Object content = List.of(
                Map.of("type", "text", "text", prompt),
                Map.of("type", "image_url", "image_url", Map.of("url", dataUrl)));
        return chat(List.of(Map.of("role", "user", "content", content)), false);
    }

    @Override
    public String analyzeVideoContext(String prompt, Map<String, Object> videoContext) {
        try {
            String contextJson = objectMapper.writeValueAsString(videoContext);
            return generateText(prompt + "\n\nVIDEO_CONTEXT_JSON:\n" + contextJson);
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            throw new AiException("AI_PROVIDER_ERROR", "Unable to serialize video context", e);
        }
    }

    Map<String, Object> buildChatBody(Object messages, boolean jsonMode) {
        var body = new java.util.HashMap<String, Object>();
        body.put("model", properties.model());
        body.put("messages", messages);
        body.put("temperature", 0.7);
        if (jsonMode) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        return body;
    }

    private String chat(Object messages, boolean jsonMode) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new AiException("AI_PROVIDER_ERROR",
                    "AI API key is not configured (env AI_API_KEY)");
        }
        try {
            Map<String, Object> body = buildChatBody(messages, jsonMode);
            String raw = restClient.post()
                    .uri("/chat/completions")
                    .headers(h -> h.setBearerAuth(properties.apiKey()))
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(raw);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                throw new AiException("AI_PROVIDER_ERROR", "AI returned empty response");
            }
            log.info("AI_GENERATION_OK model={} jsonMode={}", properties.model(), jsonMode);
            return content.asText();
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            throw new AiException("AI_PROVIDER_ERROR", "AI provider call failed", e);
        }
    }
}
