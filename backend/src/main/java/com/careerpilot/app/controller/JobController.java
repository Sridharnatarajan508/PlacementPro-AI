package com.careerpilot.app.controller;

import com.careerpilot.app.dto.request.JobSearchRequest;
import com.careerpilot.app.dto.response.JobMatchResponse;
import com.careerpilot.app.dto.response.JobRecommendationResponse;
import com.careerpilot.app.dto.response.JobResponse;
import com.careerpilot.app.service.JobMatchingService;
import com.careerpilot.app.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Jobs", description = "Job Search, Saved Jobs, and AI Matching APIs")
@SecurityRequirement(name = "bearerAuth")
public class JobController {

    private final JobService jobService;
    private final JobMatchingService jobMatchingService;

    @GetMapping
    @Operation(summary = "Search and filter job postings")
    public ResponseEntity<List<JobResponse>> getJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String experienceLevel,
            @RequestParam(required = false) Integer minimumMatchScore,
            Authentication authentication) {

        JobSearchRequest request = JobSearchRequest.builder()
                .keyword(keyword)
                .location(location)
                .jobType(jobType)
                .experienceLevel(experienceLevel)
                .minimumMatchScore(minimumMatchScore)
                .build();

        String userEmail = authentication != null ? authentication.getName() : null;
        List<JobResponse> jobs = jobService.getJobs(request, userEmail);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/recommended")
    @Operation(summary = "Get AI-ranked recommended jobs for candidate")
    public ResponseEntity<List<JobRecommendationResponse>> getRecommendedJobs(Authentication authentication) {
        String userEmail = authentication.getName();
        List<JobRecommendationResponse> recommended = jobMatchingService.getRecommendedJobs(userEmail);
        return ResponseEntity.ok(recommended);
    }

    @GetMapping("/saved")
    @Operation(summary = "Get all saved jobs for current user")
    public ResponseEntity<List<JobResponse>> getSavedJobs(Authentication authentication) {
        String userEmail = authentication.getName();
        List<JobResponse> savedJobs = jobService.getSavedJobs(userEmail);
        return ResponseEntity.ok(savedJobs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job details by ID")
    public ResponseEntity<JobResponse> getJobById(
            @PathVariable Long id,
            Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : null;
        JobResponse job = jobService.getJobById(id, userEmail);
        return ResponseEntity.ok(job);
    }

    @PostMapping("/{id}/match")
    @Operation(summary = "Perform or retrieve cached AI job match analysis")
    public ResponseEntity<JobMatchResponse> matchJob(
            @PathVariable Long id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        JobMatchResponse match = jobMatchingService.matchJob(id, userEmail);
        return ResponseEntity.ok(match);
    }

    @GetMapping("/{id}/match")
    @Operation(summary = "Get existing AI job match analysis for job")
    public ResponseEntity<JobMatchResponse> getMatchForJob(
            @PathVariable Long id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        JobMatchResponse match = jobMatchingService.getMatchForJob(id, userEmail);
        return ResponseEntity.ok(match);
    }

    @PostMapping("/{id}/save")
    @Operation(summary = "Bookmark / save a job")
    public ResponseEntity<JobResponse> saveJob(
            @PathVariable Long id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        JobResponse saved = jobService.saveJob(id, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/{id}/save")
    @Operation(summary = "Unsave / remove bookmarked job")
    public ResponseEntity<Map<String, String>> unsaveJob(
            @PathVariable Long id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        jobService.unsaveJob(id, userEmail);
        return ResponseEntity.ok(Map.of("message", "Job successfully removed from saved list"));
    }
}
