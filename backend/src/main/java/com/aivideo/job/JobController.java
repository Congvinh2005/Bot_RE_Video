package com.aivideo.job;

import com.aivideo.common.exception.ResourceNotFoundException;
import com.aivideo.job.dto.FullGenerateRequest;
import com.aivideo.job.dto.JobResponse;
import com.aivideo.job.dto.JobStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{id}")
@RequiredArgsConstructor
public class JobController {

    private final JobOrchestrator jobOrchestrator;
    private final JobRunner jobRunner;
    private final VideoJobRepository videoJobRepository;

    @PostMapping("/generate-full")
    public ResponseEntity<JobResponse> generateFull(
            @PathVariable UUID id,
            @RequestBody(required = false) FullGenerateRequest request) {
        String userContext = request != null ? request.userContext() : null;
        VideoJob job = jobOrchestrator.launchFullPipeline(id, userContext);
        jobRunner.run(job.getId(), userContext);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new JobResponse(job.getId(), job.getStatus()));
    }

    @GetMapping("/status")
    public ResponseEntity<JobStatusResponse> status(@PathVariable UUID id) {
        VideoJob job = latestJob(id);
        return ResponseEntity.ok(JobStatusResponse.from(job));
    }

    @GetMapping(value = "/status/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter statusStream(@PathVariable UUID id) {
        SseEmitter emitter = new SseEmitter(300_000L);
        try {
            for (int i = 0; i < 300; i++) {
                VideoJob job = latestJob(id);
                emitter.send(JobStatusResponse.from(job));
                if (job.getStatus() == JobStatus.COMPLETED
                        || job.getStatus() == JobStatus.FAILED
                        || job.getStatus() == JobStatus.CANCELLED) {
                    break;
                }
                Thread.sleep(1000);
            }
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    private VideoJob latestJob(UUID projectId) {
        return videoJobRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("JOB_NOT_FOUND",
                        "No job for project: " + projectId));
    }
}
