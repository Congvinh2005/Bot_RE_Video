package com.aivideo.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Chạy full pipeline background trên executor riêng. Lỗi đã ghi vào VideoJob. */
@Component
@RequiredArgsConstructor
@Slf4j
public class JobRunner {

    private final JobOrchestrator jobOrchestrator;

    @Async("analysisExecutor")
    public void run(UUID jobId, String userContext) {
        try {
            jobOrchestrator.runFullPipeline(jobId, userContext);
        } catch (Exception e) {
            log.warn("JOB_RUNNER_FAILED job={} error={}", jobId, e.getMessage());
        }
    }
}
