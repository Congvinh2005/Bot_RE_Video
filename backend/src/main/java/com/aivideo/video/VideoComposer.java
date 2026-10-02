package com.aivideo.video;

import com.aivideo.source.VideoMetadata;
import com.aivideo.source.VideoMetadataService;
import com.aivideo.source.VideoUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Ghép video dọc 9:16 chuẩn TikTok: scale/crop 1080x1920, H.264 + AAC,
 * thay audio gốc bằng voice ADAM (loudnorm), burn subtitle, overlay text
 * theo scene, cắt tối đa 30s. Mọi command do backend tự dựng từ validated
 * params — frontend không bao giờ gửi raw FFmpeg command.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VideoComposer {

    public record Overlay(String text, double start, double end) {
    }

    private final VideoProperties properties;
    private final VideoMetadataService metadataService;

    public Path compose(Path sourceVideo, Path voiceAudio, Path subtitleFile,
                        List<Overlay> overlays, double voiceDurationSeconds) {
        if (!Files.exists(Path.of(properties.fontPath()))) {
            throw new VideoUploadException("FFMPEG_ERROR",
                    "Font file not found for overlay: " + properties.fontPath(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        try {
            VideoMetadata sourceMeta = metadataService.probe(sourceVideo);
            double targetDuration = Math.min(voiceDurationSeconds,
                    properties.maxDurationSeconds());
            boolean loop = sourceMeta.duration() != null
                    && sourceMeta.duration() < voiceDurationSeconds;

            Path output = sourceVideo.getParent()
                    .resolve("final-" + System.nanoTime() + ".mp4");
            List<String> command = new ArrayList<>();
            command.add(properties.ffmpegPath());
            command.addAll(List.of("-y", "-v", "error"));
            if (loop) {
                command.addAll(List.of("-stream_loop", "-1"));
            }
            command.addAll(List.of("-i", sourceVideo.toString(), "-i", voiceAudio.toString()));
            command.addAll(List.of("-filter_complex", buildFilter(subtitleFile, overlays)));
            command.addAll(List.of(
                    "-map", "[vout]", "-map", "[aout]",
                    "-c:v", "libx264", "-pix_fmt", "yuv420p",
                    "-preset", "veryfast", "-crf", "23",
                    "-r", String.valueOf(properties.fps()),
                    "-c:a", "aac", "-b:a", "128k", "-ar", "44100", "-ac", "2",
                    "-shortest", "-t", String.valueOf((int) Math.ceil(targetDuration)),
                    output.toString()));

            Process process = new ProcessBuilder(command).start();
            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new VideoUploadException("FFMPEG_ERROR", "Video composition timed out",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
            if (process.exitValue() != 0 || !Files.exists(output) || Files.size(output) == 0) {
                throw new VideoUploadException("FFMPEG_ERROR", "Video composition failed",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
            log.info("VIDEO_COMPOSE_OK output={} {}x{}",
                    output.getFileName(), properties.width(), properties.height());
            return output;
        } catch (VideoUploadException e) {
            throw e;
        } catch (Exception e) {
            throw new VideoUploadException("FFMPEG_ERROR", "Video composition failed",
                    e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String buildFilter(Path subtitleFile, List<Overlay> overlays) {
        StringBuilder video = new StringBuilder();
        video.append("[0:v]scale=").append(properties.width()).append(':')
                .append(properties.height())
                .append(":force_original_aspect_ratio=increase")
                .append(",crop=").append(properties.width()).append(':')
                .append(properties.height())
                .append(",setsar=1,fps=").append(properties.fps());
        video.append(",subtitles=").append(escapePath(subtitleFile.toString()));
        for (Overlay overlay : overlays) {
            if (overlay.text() == null || overlay.text().isBlank()) {
                continue;
            }
            video.append(",drawtext=fontfile=").append(escapePath(properties.fontPath()))
                    .append(":text=").append(escapeText(overlay.text()))
                    .append(":fontsize=64:fontcolor=white")
                    .append(":borderw=3:bordercolor=black")
                    .append(":x=(w-text_w)/2:y=h-280")
                    .append(":enable='between(t\\,")
                    .append(overlay.start()).append('\\').append(',').append(overlay.end())
                    .append(")'");
        }
        video.append("[vout]");
        video.append(";[1:a]loudnorm=I=-16:TP=-1.5:LRA=11,aresample=44100")
                .append(",aformat=channel_layouts=stereo[aout]");
        return video.toString();
    }

    private String escapePath(String path) {
        return "'" + path.replace("\\", "\\\\").replace("'", "\\'").replace(":", "\\:") + "'";
    }

    private String escapeText(String text) {
        String singleLine = text.replace("\n", " ").replace("\r", " ").strip();
        String safe = singleLine.length() > 120 ? singleLine.substring(0, 120) : singleLine;
        return "'" + safe.replace("\\", "\\\\").replace("'", "\\'").replace(":", "\\:")
                .replace("%", "\\\\%") + "'";
    }
}
