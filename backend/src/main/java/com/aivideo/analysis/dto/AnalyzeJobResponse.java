package com.aivideo.analysis.dto;

import com.aivideo.job.JobStatus;

import java.util.UUID;

public record AnalyzeJobResponse(
        UUID jobId,
        JobStatus status
) {
}
