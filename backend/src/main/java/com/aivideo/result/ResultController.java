package com.aivideo.result;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{id}")
@RequiredArgsConstructor
public class ResultController {

    private final ResultService resultService;

    @GetMapping("/result")
    public ResponseEntity<ProjectResultResponse> getResult(@PathVariable UUID id) {
        return ResponseEntity.ok(resultService.getResult(id));
    }
}
