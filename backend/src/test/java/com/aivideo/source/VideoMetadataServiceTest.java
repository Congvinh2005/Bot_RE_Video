package com.aivideo.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VideoMetadataServiceTest {

    @TempDir
    Path tempDir;

    private VideoMetadataService service;

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(isAvailable("ffprobe", "-version"),
                "ffprobe not available, skipping");
        UploadProperties props = new UploadProperties(500,
                List.of("mp4", "mov", "webm"),
                List.of("video/mp4", "video/quicktime", "video/webm"),
                "ffprobe");
        service = new VideoMetadataService(props, new ObjectMapper());
    }

    @Test
    void probeExtractsMetadataFromRealVideo() throws Exception {
        Assumptions.assumeTrue(isAvailable("ffmpeg", "-version"),
                "ffmpeg not available, skipping");
        Path video = tempDir.resolve("sample.mp4");
        run("ffmpeg", "-y", "-v", "error",
                "-f", "lavfi", "-i", "testsrc=duration=1:size=640x480:rate=30",
                "-f", "lavfi", "-i", "sine=frequency=1000:duration=1",
                "-pix_fmt", "yuv420p", "-shortest", video.toString());

        VideoMetadata meta = service.probe(video);

        assertThat(meta.width()).isEqualTo(640);
        assertThat(meta.height()).isEqualTo(480);
        assertThat(meta.fps()).isBetween(29.0, 31.0);
        assertThat(meta.videoCodec()).isEqualTo("h264");
        assertThat(meta.hasAudio()).isTrue();
        assertThat(meta.duration()).isBetween(0.9, 1.5);
        assertThat(meta.format()).contains("mp4");
    }

    @Test
    void probeInvalidFileThrowsFfmpegError() throws Exception {
        Path bad = tempDir.resolve("bad.mp4");
        Files.write(bad, "not a video".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.probe(bad))
                .isInstanceOf(VideoUploadException.class)
                .satisfies(ex -> assertThat(((VideoUploadException) ex).getCode())
                        .isEqualTo("FFMPEG_ERROR"));
    }

    @Test
    void parseFrameRateHandlesEdgeCases() {
        assertThat(VideoMetadataService.parseFrameRate("30/1")).isEqualTo(30.0);
        assertThat(VideoMetadataService.parseFrameRate("30000/1001")).isBetween(29.9, 30.0);
        assertThat(VideoMetadataService.parseFrameRate("0/0")).isNull();
        assertThat(VideoMetadataService.parseFrameRate(null)).isNull();
        assertThat(VideoMetadataService.parseFrameRate("abc")).isNull();
    }

    private boolean isAvailable(String... command) {
        try {
            return new ProcessBuilder(command).start().waitFor(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            return false;
        }
    }

    private void run(String... command) throws Exception {
        Process process = new ProcessBuilder(command).start();
        boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        if (!finished || process.exitValue() != 0) {
            throw new IllegalStateException("Command failed: " + String.join(" ", command));
        }
    }
}
