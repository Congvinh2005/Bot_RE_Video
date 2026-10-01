package com.aivideo.analysis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Optional;

/** Chưa cấu hình STT thật: trả rỗng để pipeline chạy tiếp, log rõ ràng. */
@Component
@Slf4j
public class NoopSttProvider implements SttProvider {

    @Override
    public Optional<String> transcribe(Path video) {
        log.info("STT_SKIPPED file={} reason=no-provider-configured", video.getFileName());
        return Optional.empty();
    }
}
