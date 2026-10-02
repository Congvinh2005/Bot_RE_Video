package com.aivideo.video;

import com.aivideo.source.UploadProperties;
import com.aivideo.source.VideoMetadataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class VideoComposerTest {

    static final String FULL_FFMPEG = "/opt/homebrew/opt/ffmpeg-full/bin/ffmpeg";
    static final String FULL_FFPROBE = "/opt/homebrew/opt/ffmpeg-full/bin/ffprobe";

    @TempDir
    Path tempDir;

    VideoComposer composer;
    VideoMetadataService metadataService;

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(new File(FULL_FFMPEG).exists()
                        && new File("/System/Library/Fonts/Helvetica.ttc").exists(),
                "full ffmpeg or font missing, skipping");
        VideoProperties props = new VideoProperties(FULL_FFMPEG,
                "/System/Library/Fonts/Helvetica.ttc", 1080, 1920, 30, 30);
        UploadProperties uploadProps = new UploadProperties(500,
                List.of("mp4"), List.of("video/mp4"), FULL_FFPROBE);
        metadataService = new VideoMetadataService(uploadProps, new ObjectMapper());
        composer = new VideoComposer(props, metadataService);
    }

    private void makeSource(Path out) throws Exception {
        run(FULL_FFMPEG, "-y", "-v", "error", "-f", "lavfi",
                "-i", "testsrc=duration=6:size=640x480:rate=30",
                "-pix_fmt", "yuv420p", out.toString());
    }

    private void makeVoice(Path out, int seconds) throws Exception {
        run(FULL_FFMPEG, "-y", "-v", "error", "-f", "lavfi",
                "-i", "sine=frequency=440:duration=" + seconds,
                "-c:a", "libmp3lame", out.toString());
    }

    @Test
    void composeProduces1080x1920WithVoiceAndSubtitle() throws Exception {
        Path source = tempDir.resolve("source.mp4");
        Path voice = tempDir.resolve("voice.mp3");
        Path subs = tempDir.resolve("subs.srt");
        makeSource(source);
        makeVoice(voice, 4);
        Files.write(subs, ("1\n00:00:00,000 --> 00:00:02,000\nHello\n\n"
                + "2\n00:00:02,000 --> 00:00:04,000\nWorld\n").getBytes(StandardCharsets.UTF_8));

        Path output = composer.compose(source, voice, subs,
                List.of(new VideoComposer.Overlay("Mua ngay", 0, 2),
                        new VideoComposer.Overlay("Giam 50%", 2, 4)),
                4.0);

        assertThat(Files.exists(output)).isTrue();
        assertThat(Files.size(output)).isGreaterThan(10_000);
        var meta = metadataService.probe(output);
        assertThat(meta.width()).isEqualTo(1080);
        assertThat(meta.height()).isEqualTo(1920);
        assertThat(meta.videoCodec()).isEqualTo("h264");
        assertThat(meta.hasAudio()).isTrue();
        assertThat(meta.duration()).isBetween(3.5, 4.5);
    }

    @Test
    void composeCapsAtMaxDuration() throws Exception {
        Path source = tempDir.resolve("source.mp4");
        Path voice = tempDir.resolve("voice.mp3");
        Path subs = tempDir.resolve("subs.srt");
        makeSource(source);
        makeVoice(voice, 6);
        Files.write(subs, "1\n00:00:00,000 --> 00:00:06,000\nHi\n"
                .getBytes(StandardCharsets.UTF_8));

        VideoProperties capped = new VideoProperties(FULL_FFMPEG,
                "/System/Library/Fonts/Helvetica.ttc", 1080, 1920, 30, 5);
        VideoComposer cappedComposer = new VideoComposer(capped, metadataService);

        Path output = cappedComposer.compose(source, voice, subs, List.of(), 6.0);

        var meta = metadataService.probe(output);
        assertThat(meta.duration()).isLessThanOrEqualTo(5.5);
    }

    private void run(String... command) throws Exception {
        Process process = new ProcessBuilder(command).start();
        if (!process.waitFor(120, TimeUnit.SECONDS) || process.exitValue() != 0) {
            throw new IllegalStateException("Command failed: " + String.join(" ", command));
        }
    }
}
