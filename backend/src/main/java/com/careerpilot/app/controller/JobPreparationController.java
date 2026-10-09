package com.careerpilot.app.controller;

import com.careerpilot.app.dto.response.JobPreparationResponse;
import com.careerpilot.app.service.JobPreparationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/job-preparation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Job Preparation", description = "AI Job Preparation & Personalized Interview Preparation")
@SecurityRequirement(name = "bearerAuth")
public class JobPreparationController {

    private final JobPreparationService jobPreparationService;

    @GetMapping
    @Operation(summary = "Get all user job preparation plans")
    public ResponseEntity<List<JobPreparationResponse>> getAllPreparations(Authentication authentication) {
        String userEmail = authentication.getName();
        List<JobPreparationResponse> list = jobPreparationService.getAllPreparations(userEmail);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Get job preparation plan for a specific job")
    public ResponseEntity<JobPreparationResponse> getPreparation(
            @PathVariable Long jobId,
            Authentication authentication) {
        String userEmail = authentication.getName();
        JobPreparationResponse response = jobPreparationService.getPreparation(jobId, userEmail);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{jobId}/generate")
    @Operation(summary = "Generate job preparation plan (cached if already exists)")
    public ResponseEntity<JobPreparationResponse> generatePreparation(
            @PathVariable Long jobId,
            Authentication authentication) {
        String userEmail = authentication.getName();
        JobPreparationResponse response = jobPreparationService.generatePreparation(jobId, userEmail);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{jobId}/regenerate")
    @Operation(summary = "Force regenerate AI job preparation plan via Gemini")
    public ResponseEntity<JobPreparationResponse> regeneratePreparation(
            @PathVariable Long jobId,
            Authentication authentication) {
        String userEmail = authentication.getName();
        JobPreparationResponse response = jobPreparationService.regeneratePreparation(jobId, userEmail);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{jobId}")
    @Operation(summary = "Delete a job preparation plan")
    public ResponseEntity<Map<String, String>> deletePreparation(
            @PathVariable Long jobId,
            Authentication authentication) {
        String userEmail = authentication.getName();
        jobPreparationService.deletePreparation(jobId, userEmail);
        return ResponseEntity.ok(Map.of("message", "Job preparation plan successfully deleted"));
    }
}
