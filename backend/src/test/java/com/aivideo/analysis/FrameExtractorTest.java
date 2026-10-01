package com.aivideo.analysis;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class FrameExtractorTest {

    @TempDir
    Path tempDir;

    FrameExtractor extractor;

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(canRun("ffmpeg", "-version"), "ffmpeg missing, skipping");
        extractor = new FrameExtractor(new AnalysisProperties(0.4, 5, "ffmpeg", 1, "vi"));
    }

    @Test
    void extractsJpegFrame() throws Exception {
        Path video = tempDir.resolve("sample.mp4");
        Process ffmpeg = new ProcessBuilder("ffmpeg", "-y", "-v", "error",
                "-f", "lavfi", "-i", "testsrc=duration=1:size=320x240:rate=30",
                "-pix_fmt", "yuv420p", video.toString()).start();
        Assumptions.assumeTrue(ffmpeg.waitFor(60, TimeUnit.SECONDS) && ffmpeg.exitValue() == 0);

        byte[] jpeg = extractor.extractFrame(video, 0.5);

        assertThat(jpeg.length).isGreaterThan(100);
        assertThat(jpeg[0]).isEqualTo((byte) 0xFF);
        assertThat(jpeg[1]).isEqualTo((byte) 0xD8);
    }

    private boolean canRun(String... command) {
        try {
            return new ProcessBuilder(command).start().waitFor(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            return false;
        }
    }
}
