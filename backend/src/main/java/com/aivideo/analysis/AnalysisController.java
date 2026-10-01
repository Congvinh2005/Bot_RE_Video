package com.aivideo.analysis;

import com.aivideo.analysis.dto.AnalyzeJobResponse;
import com.aivideo.analysis.dto.AnalyzeRequest;
import com.aivideo.job.VideoJob;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{id}")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;
    private final AnalysisRunner analysisRunner;

    @PostMapping("/analyze")
    public ResponseEntity<AnalyzeJobResponse> analyze(
            @PathVariable UUID id,
            @RequestBody(required = false) AnalyzeRequest request) {
        UUID sourceId = request != null ? request.videoSourceId() : null;
        VideoJob job = analysisService.launchAnalysis(id, sourceId);
        analysisRunner.run(job.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new AnalyzeJobResponse(job.getId(), job.getStatus()));
    }
}
