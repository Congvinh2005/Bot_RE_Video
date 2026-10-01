package com.aivideo.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Chạy FFprobe binary để trích metadata. Mọi tham số do backend tự xây dựng,
 * frontend không bao giờ được truyền command.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoMetadataService {

    private final UploadProperties properties;
    private final ObjectMapper objectMapper;

    public VideoMetadata probe(Path file) {
        try {
            Process process = new ProcessBuilder(
                    properties.ffprobePath(),
                    "-v", "error",
                    "-print_format", "json",
                    "-show_format", "-show_streams",
                    file.toAbsolutePath().toString())
                    .start();
            String json = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new VideoUploadException("FFMPEG_ERROR", "FFprobe timed out",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
            if (process.exitValue() != 0) {
                throw new VideoUploadException("FFMPEG_ERROR", "FFprobe failed to read video",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
            VideoMetadata metadata = parse(json);
            log.info("FFPROBE_OK file={} duration={} {}x{} fps={}",
                    file.getFileName(), metadata.duration(),
                    metadata.width(), metadata.height(), metadata.fps());
            return metadata;
        } catch (VideoUploadException e) {
            throw e;
        } catch (Exception e) {
            throw new VideoUploadException("FFMPEG_ERROR", "Unable to probe video",
                    e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private VideoMetadata parse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode format = root.path("format");
        Double duration = format.has("duration") ? format.get("duration").asDouble() : null;
        String formatName = format.has("format_name") ? format.get("format_name").asText() : null;

        Integer width = null;
        Integer height = null;
        Double fps = null;
        String videoCodec = null;
        String audioCodec = null;
        boolean hasAudio = false;

        for (JsonNode stream : root.path("streams")) {
            String codecType = stream.path("codec_type").asText("");
            if ("video".equals(codecType) && videoCodec == null) {
                width = stream.has("width") ? stream.get("width").asInt() : null;
                height = stream.has("height") ? stream.get("height").asInt() : null;
                fps = parseFrameRate(stream.path("avg_frame_rate").asText(null));
                videoCodec = stream.path("codec_name").asText(null);
            } else if ("audio".equals(codecType) && !hasAudio) {
                hasAudio = true;
                audioCodec = stream.path("codec_name").asText(null);
            }
        }
        return new VideoMetadata(duration, width, height, fps,
                videoCodec, audioCodec, hasAudio, formatName);
    }

    static Double parseFrameRate(String rate) {
        if (rate == null || rate.isBlank() || "0/0".equals(rate)) {
            return null;
        }
        String[] parts = rate.split("/");
        try {
            double num = Double.parseDouble(parts[0]);
            double den = parts.length > 1 ? Double.parseDouble(parts[1]) : 1;
            return den == 0 ? null : num / den;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
