package com.aivideo.job.dto;

import com.aivideo.job.JobStatus;

import java.util.UUID;

public record JobResponse(
        UUID jobId,
        JobStatus status
) {
}
