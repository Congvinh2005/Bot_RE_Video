package com.aivideo.analysis;

import java.nio.file.Path;
import java.util.Optional;

/** Abstraction speech-to-text. Provider thật (Whisper...) sẽ thay Noop sau. */
public interface SttProvider {

    Optional<String> transcribe(Path video);
}
