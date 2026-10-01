package com.aivideo.voice;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ElevenLabsVoiceProviderTest {

    HttpServer server;
    AtomicReference<byte[]> responseAudio;
    AtomicReference<String> capturedRequest;
    AtomicReference<String> capturedQuery;
    AtomicReference<String> capturedVoiceId;
    AtomicReference<Integer> statusCode;
    ElevenLabsVoiceProvider provider;

    @BeforeEach
    void setUp() throws IOException {
        responseAudio = new AtomicReference<>(new byte[]{1, 2, 3, 4});
        capturedRequest = new AtomicReference<>("");
        capturedQuery = new AtomicReference<>("");
        capturedVoiceId = new AtomicReference<>("");
        statusCode = new AtomicReference<>(200);
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/text-to-speech", exchange -> {
            String path = exchange.getRequestURI().getPath();
            capturedVoiceId.set(path.substring(path.lastIndexOf('/') + 1));
            capturedQuery.set(exchange.getRequestURI().getQuery());
            capturedRequest.set(new String(exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8));
            byte[] audio = responseAudio.get();
            exchange.getResponseHeaders().add("Content-Type", "audio/mpeg");
            exchange.sendResponseHeaders(statusCode.get(), audio.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(audio);
            }
        });
        server.start();
        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        provider = new ElevenLabsVoiceProvider(
                new ElevenLabsProperties(baseUrl, "test-key", "voice-123",
                        "eleven_v3", "vi", "mp3_44100_128", 10));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void voiceNameIsAdam() {
        assertThat(provider.voiceName()).isEqualTo("ADAM");
    }

    @Test
    void generateSpeechPostsCorrectPayload() {
        byte[] audio = provider.generateSpeech("Xin chao", VoiceOptions.defaults());

        assertThat(audio).isEqualTo(new byte[]{1, 2, 3, 4});
        assertThat(capturedVoiceId.get()).isEqualTo("voice-123");
        assertThat(capturedRequest.get()).contains("Xin chao");
        assertThat(capturedRequest.get()).contains("eleven_v3");
        assertThat(capturedRequest.get()).contains("\"language_code\":\"vi\"");
        assertThat(capturedQuery.get()).contains("output_format=mp3_44100_128");
    }

    @Test
    void missingApiKeyFailsWithoutHttpCall() {
        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        ElevenLabsVoiceProvider noKey = new ElevenLabsVoiceProvider(
                new ElevenLabsProperties(baseUrl, "", "voice-123",
                        "eleven_v3", "vi", "mp3_44100_128", 10));

        assertThatThrownBy(() -> noKey.generateSpeech("hi", VoiceOptions.defaults()))
                .isInstanceOf(VoiceException.class)
                .satisfies(ex -> assertThat(((VoiceException) ex).getCode())
                        .isEqualTo("TTS_PROVIDER_ERROR"));
        assertThat(capturedRequest.get()).isEmpty();
    }

    @Test
    void httpErrorMapsToVoiceException() {
        statusCode.set(401);
        responseAudio.set("unauthorized".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> provider.generateSpeech("hi", VoiceOptions.defaults()))
                .isInstanceOf(VoiceException.class)
                .satisfies(ex -> assertThat(((VoiceException) ex).getCode())
                        .isEqualTo("TTS_PROVIDER_ERROR"));
    }

    @Test
    void blankTextFails() {
        assertThatThrownBy(() -> provider.generateSpeech("  ", VoiceOptions.defaults()))
                .isInstanceOf(VoiceException.class);
    }
}
