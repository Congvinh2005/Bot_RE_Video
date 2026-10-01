package com.aivideo.analysis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Chạy analysis background trên executor riêng. Lỗi đã ghi vào VideoJob. */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalysisRunner {

    private final AnalysisService analysisService;

    @Async("analysisExecutor")
    public void run(UUID jobId) {
        try {
            analysisService.doAnalyze(jobId);
        } catch (Exception e) {
            log.warn("ANALYSIS_RUNNER_FAILED job={} error={}", jobId, e.getMessage());
        }
    }
}
