package com.aivideo.job.dto;

import com.aivideo.job.JobStatus;
import com.aivideo.job.JobStep;
import com.aivideo.job.VideoJob;

import java.util.UUID;

public record JobStatusResponse(
        UUID jobId,
        JobStatus status,
        int progress,
        JobStep currentStep,
        String errorMessage
) {
    public static JobStatusResponse from(VideoJob job) {
        return new JobStatusResponse(
                job.getId(),
                job.getStatus(),
                job.getProgress() != null ? job.getProgress() : 0,
                job.getCurrentStep(),
                job.getErrorMessage());
    }
}
