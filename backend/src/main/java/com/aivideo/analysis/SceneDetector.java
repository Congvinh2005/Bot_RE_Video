package com.aivideo.analysis;

import java.nio.file.Path;
import java.util.List;

/** Phát hiện scene bằng ffmpeg select filter. Không nhận command từ frontend. */
public interface SceneDetector {

    List<SceneSegment> detect(Path video, double duration);
}
