package com.aivideo.analysis;

import com.aivideo.source.VideoUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Scene detect thật bằng ffmpeg select='gt(scene,t)' + showinfo. */
@Component
@RequiredArgsConstructor
@Slf4j
public class FfmpegSceneDetector implements SceneDetector {

    private static final Pattern PTS_TIME = Pattern.compile("pts_time:([0-9.]+)");

    private final AnalysisProperties properties;

    @Override
    public List<SceneSegment> detect(Path video, double duration) {
        try {
            String filter = "select='gt(scene," + properties.sceneThreshold() + ")',showinfo";
            Process process = new ProcessBuilder(
                    properties.ffmpegPath(), "-v", "info", "-i",
                    video.toAbsolutePath().toString(),
                    "-filter:v", filter, "-f", "null", "-")
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new VideoUploadException("FFMPEG_ERROR", "Scene detection timed out",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
            List<Double> cuts = new ArrayList<>();
            cuts.add(0.0);
            Matcher matcher = PTS_TIME.matcher(output);
            while (matcher.find()) {
                double t = Double.parseDouble(matcher.group(1));
                if (t > 0.1 && t < duration - 0.1) {
                    cuts.add(t);
                }
            }
            cuts.add(duration);
            List<SceneSegment> scenes = new ArrayList<>();
            for (int i = 0; i < cuts.size() - 1; i++) {
                scenes.add(new SceneSegment(cuts.get(i), cuts.get(i + 1)));
            }
            log.info("SCENE_DETECT_OK file={} scenes={}", video.getFileName(), scenes.size());
            return scenes;
        } catch (VideoUploadException e) {
            throw e;
        } catch (Exception e) {
            throw new VideoUploadException("FFMPEG_ERROR", "Scene detection failed",
                    e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
