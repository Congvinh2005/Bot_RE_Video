package com.aivideo.analysis;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class FfmpegSceneDetectorTest {

    @TempDir
    Path tempDir;

    FfmpegSceneDetector detector;

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(canRun("ffmpeg", "-version"), "ffmpeg missing, skipping");
        detector = new FfmpegSceneDetector(new AnalysisProperties(0.4, 5, "ffmpeg", 1, "vi"));
    }

    @Test
    void detectsMultipleScenesOnColorChange() throws Exception {
        Path red = tempDir.resolve("red.mp4");
        Path blue = tempDir.resolve("blue.mp4");
        Path joined = tempDir.resolve("joined.mp4");
        makeColor(red, "red", 1);
        makeColor(blue, "blue", 1);
        run("ffmpeg", "-y", "-v", "error", "-i", red.toString(), "-i", blue.toString(),
                "-filter_complex", "[0:v][1:v]concat=n=2:v=1:a=0", "-pix_fmt", "yuv420p",
                joined.toString());

        List<SceneSegment> scenes = detector.detect(joined, 2.0);

        assertThat(scenes.size()).isGreaterThanOrEqualTo(2);
        assertThat(scenes.get(0).start()).isEqualTo(0.0);
        assertThat(scenes.get(scenes.size() - 1).end()).isEqualTo(2.0);
    }

    @Test
    void singleSceneForUniformVideo() throws Exception {
        Path red = tempDir.resolve("red.mp4");
        makeColor(red, "red", 1);

        List<SceneSegment> scenes = detector.detect(red, 1.0);

        assertThat(scenes).hasSize(1);
        assertThat(scenes.get(0).start()).isEqualTo(0.0);
        assertThat(scenes.get(0).end()).isEqualTo(1.0);
    }

    private void makeColor(Path out, String color, int seconds) throws Exception {
        run("ffmpeg", "-y", "-v", "error", "-f", "lavfi",
                "-i", "color=" + color + ":duration=" + seconds + ":size=320x240:rate=30",
                "-pix_fmt", "yuv420p", out.toString());
    }

    private boolean canRun(String... command) {
        try {
            return new ProcessBuilder(command).start().waitFor(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            return false;
        }
    }

    private void run(String... command) throws Exception {
        Process process = new ProcessBuilder(command).start();
        if (!process.waitFor(60, TimeUnit.SECONDS) || process.exitValue() != 0) {
            throw new IllegalStateException("Command failed: " + String.join(" ", command));
        }
    }
}
