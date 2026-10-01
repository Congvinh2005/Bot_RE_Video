package com.aivideo.analysis;

import com.aivideo.source.VideoUploadException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/** Trích 1 frame JPEG tại timestamp cho AI vision. */
@Component
@RequiredArgsConstructor
public class FrameExtractor {

    private final AnalysisProperties properties;

    public byte[] extractFrame(Path video, double timestampSeconds) {
        try {
            Process process = new ProcessBuilder(
                    properties.ffmpegPath(),
                    "-v", "error",
                    "-ss", String.valueOf(timestampSeconds),
                    "-i", video.toAbsolutePath().toString(),
                    "-frames:v", "1", "-q:v", "3",
                    "-f", "image2pipe", "-vcodec", "mjpeg", "pipe:1")
                    .start();
            byte[] jpeg = process.getInputStream().readAllBytes();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new VideoUploadException("FFMPEG_ERROR", "Frame extraction timed out",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
            if (process.exitValue() != 0 || jpeg.length == 0) {
                throw new VideoUploadException("FFMPEG_ERROR", "Frame extraction failed",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return jpeg;
        } catch (VideoUploadException e) {
            throw e;
        } catch (Exception e) {
            throw new VideoUploadException("FFMPEG_ERROR", "Frame extraction failed",
                    e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
