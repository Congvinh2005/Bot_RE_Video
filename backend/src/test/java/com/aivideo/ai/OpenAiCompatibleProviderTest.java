package com.aivideo.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiCompatibleProviderTest {

    HttpServer server;
    AtomicReference<String> responseBody;
    AtomicReference<String> capturedRequest;
    AtomicReference<Integer> statusCode;
    OpenAiCompatibleProvider provider;

    record HookResult(String hook, int count) {
    }

    @BeforeEach
    void setUp() throws IOException {
        responseBody = new AtomicReference<>("{\"choices\":[{\"message\":{\"content\":\"hi\"}}]}");
        capturedRequest = new AtomicReference<>("");
        statusCode = new AtomicReference<>(200);
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            byte[] request = exchange.getRequestBody().readAllBytes();
            capturedRequest.set(new String(request, StandardCharsets.UTF_8));
            byte[] response = responseBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode.get(), response.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(response);
            }
        });
        server.start();
        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        AiProperties props = new AiProperties(baseUrl, "test-key", "test-model", 10);
        provider = new OpenAiCompatibleProvider(props, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void generateTextReturnsContent() {
        responseBody.set("{\"choices\":[{\"message\":{\"content\":\"hello world\"}}]}");

        assertThat(provider.generateText("hi")).isEqualTo("hello world");
        assertThat(capturedRequest.get()).contains("test-model");
    }

    @Test
    void generateStructuredParsesJson() {
        responseBody.set("{\"choices\":[{\"message\":{\"content\":\"{\\\"hook\\\":\\\"H\\\",\\\"count\\\":3}\"}}]}");

        HookResult result = provider.generateStructured("p", HookResult.class);

        assertThat(result.hook()).isEqualTo("H");
        assertThat(result.count()).isEqualTo(3);
        assertThat(capturedRequest.get()).contains("json_object");
    }

    @Test
    void generateStructuredRejectsInvalidJson() {
        responseBody.set("{\"choices\":[{\"message\":{\"content\":\"not json\"}}]}");

        assertThatThrownBy(() -> provider.generateStructured("p", HookResult.class))
                .isInstanceOf(AiException.class)
                .satisfies(ex -> assertThat(((AiException) ex).getCode())
                        .isEqualTo("AI_PROVIDER_ERROR"));
    }

    @Test
    void missingApiKeyFailsWithoutHttpCall() {
        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        OpenAiCompatibleProvider noKey = new OpenAiCompatibleProvider(
                new AiProperties(baseUrl, "", "m", 10), new ObjectMapper());

        assertThatThrownBy(() -> noKey.generateText("hi"))
                .isInstanceOf(AiException.class);
        assertThat(capturedRequest.get()).isEmpty();
    }

    @Test
    void httpErrorMapsToAiException() {
        statusCode.set(500);
        responseBody.set("boom");

        assertThatThrownBy(() -> provider.generateText("hi"))
                .isInstanceOf(AiException.class)
                .satisfies(ex -> assertThat(((AiException) ex).getCode())
                        .isEqualTo("AI_PROVIDER_ERROR"));
    }

    @Test
    void analyzeImageSendsImageUrlPart() {
        responseBody.set("{\"choices\":[{\"message\":{\"content\":\"a cat\"}}]}");

        String result = provider.analyzeImage("what?", new byte[]{1, 2, 3}, "image/jpeg");

        assertThat(result).isEqualTo("a cat");
        assertThat(capturedRequest.get()).contains("image_url");
        assertThat(capturedRequest.get()).contains("data:image/jpeg;base64,");
    }

    @Test
    void analyzeVideoContextSerializesContext() {
        responseBody.set("{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}");

        String result = provider.analyzeVideoContext("analyze",
                Map.of("duration", 15.2, "tone", "fun"));

        assertThat(result).isEqualTo("ok");
        assertThat(capturedRequest.get()).contains("15.2");
    }

    @Test
    void buildChatBodyAddsJsonModeOnlyWhenStructured() {
        Map<String, Object> plain = provider.buildChatBody(List.of("m"), false);
        Map<String, Object> json = provider.buildChatBody(List.of("m"), true);

        assertThat(plain).doesNotContainKey("response_format");
        assertThat(json.get("response_format")).isEqualTo(Map.of("type", "json_object"));
        assertThat(json.get("model")).isEqualTo("test-model");
    }
}
