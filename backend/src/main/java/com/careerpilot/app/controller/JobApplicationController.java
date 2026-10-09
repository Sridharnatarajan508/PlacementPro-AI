package com.careerpilot.app.controller;

import com.careerpilot.app.dto.ApplicationIntelligenceDto;
import com.careerpilot.app.dto.ApplicationStatsDto;
import com.careerpilot.app.dto.JobApplicationDto;
import com.careerpilot.app.dto.UpcomingTasksDto;
import com.careerpilot.app.dto.response.ApplicationResponse;
import com.careerpilot.app.entity.ApplicationStatus;
import com.careerpilot.app.service.ApplicationIntelligenceService;
import com.careerpilot.app.service.JobApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Slf4j
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;
    private final ApplicationIntelligenceService applicationIntelligenceService;

    @GetMapping
    public ResponseEntity<List<JobApplicationDto>> getAllApplications(Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("REST: Fetch all job applications for user: {}", userEmail);
        List<JobApplicationDto> list = jobApplicationService.getAllApplications(userEmail);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobApplicationDto> getApplication(
            @PathVariable("id") Long id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("REST: Fetch job application ID: {} for user: {}", id, userEmail);
        JobApplicationDto app = jobApplicationService.getApplication(id, userEmail);
        return ResponseEntity.ok(app);
    }

    @PostMapping("/job/{jobId}")
    public ResponseEntity<ApplicationResponse> createApplicationFromJob(
            @PathVariable("jobId") Long jobId,
            Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("REST: Track job ID: {} as application for user: {}", jobId, userEmail);
        JobApplicationDto app = jobApplicationService.createApplication(jobId, userEmail);
        ApplicationStatsDto stats = jobApplicationService.getStatistics(userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApplicationResponse.builder()
                        .success(true)
                        .message("Job tracked in application pipeline successfully.")
                        .application(app)
                        .statistics(stats)
                        .build()
        );
    }

    @PostMapping
    public ResponseEntity<ApplicationResponse> createCustomApplication(
            @RequestBody JobApplicationDto dto,
            Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("REST: Create custom job application for user: {} (Role: {} at {})", userEmail, dto.getJobTitle(), dto.getCompany());
        JobApplicationDto app = jobApplicationService.createCustomApplication(dto, userEmail);
        ApplicationStatsDto stats = jobApplicationService.getStatistics(userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApplicationResponse.builder()
                        .success(true)
                        .message("Application created successfully.")
                        .application(app)
                        .statistics(stats)
                        .build()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApplicationResponse> updateApplication(
            @PathVariable("id") Long id,
            @RequestBody JobApplicationDto dto,
            Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("REST: Update job application ID: {} for user: {}", id, userEmail);
        JobApplicationDto app = jobApplicationService.updateApplication(id, dto, userEmail);
        ApplicationStatsDto stats = jobApplicationService.getStatistics(userEmail);
        return ResponseEntity.ok(
                ApplicationResponse.builder()
                        .success(true)
                        .message("Application updated successfully.")
                        .application(app)
                        .statistics(stats)
                        .build()
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApplicationResponse> updateStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String userEmail = authentication.getName();
        String statusStr = body.get("status");
        if (statusStr == null || statusStr.isBlank()) {
            throw new IllegalArgumentException("Status is required.");
        }
        ApplicationStatus newStatus = ApplicationStatus.valueOf(statusStr.toUpperCase());
        log.info("REST: Update status of application ID: {} to {} for user: {}", id, newStatus, userEmail);
        JobApplicationDto app = jobApplicationService.updateStatus(id, newStatus, userEmail);
        ApplicationStatsDto stats = jobApplicationService.getStatistics(userEmail);
        return ResponseEntity.ok(
                ApplicationResponse.builder()
                        .success(true)
                        .message("Application status updated to " + newStatus + ".")
                        .application(app)
                        .statistics(stats)
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteApplication(
            @PathVariable("id") Long id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("REST: Delete job application ID: {} for user: {}", id, userEmail);
        jobApplicationService.deleteApplication(id, userEmail);
        return ResponseEntity.ok(Map.of("message", "Job application deleted successfully."));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<JobApplicationDto>> getApplicationsByStatus(
            @PathVariable("status") String statusStr,
            Authentication authentication) {
        String userEmail = authentication.getName();
        ApplicationStatus status = ApplicationStatus.valueOf(statusStr.toUpperCase());
        log.info("REST: Filter applications by status: {} for user: {}", status, userEmail);
        List<JobApplicationDto> list = jobApplicationService.getApplicationsByStatus(status, userEmail);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/stats")
    public ResponseEntity<ApplicationStatsDto> getStatistics(Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("REST: Fetch application stats for user: {}", userEmail);
        ApplicationStatsDto stats = jobApplicationService.getStatistics(userEmail);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/upcoming")
    public ResponseEntity<UpcomingTasksDto> getUpcomingTasks(Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("REST: Fetch upcoming application tasks for user: {}", userEmail);
        UpcomingTasksDto tasks = jobApplicationService.getUpcomingTasks(userEmail);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}/intelligence")
    public ResponseEntity<ApplicationIntelligenceDto> getApplicationIntelligence(
            @PathVariable("id") Long id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("REST: Fetch AI application intelligence for application ID: {} (user: {})", id, userEmail);
        ApplicationIntelligenceDto result = applicationIntelligenceService.analyzeApplication(id, userEmail);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/intelligence/regenerate")
    public ResponseEntity<ApplicationIntelligenceDto> regenerateApplicationIntelligence(
            @PathVariable("id") Long id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("REST: Regenerate AI application intelligence for application ID: {} (user: {})", id, userEmail);
        ApplicationIntelligenceDto result = applicationIntelligenceService.regenerateAnalysis(id, userEmail);
        return ResponseEntity.ok(result);
    }
}
