package com.careerpilot.app.controller;

import com.careerpilot.app.dto.response.CareerAnalyticsResponse;
import com.careerpilot.app.service.CareerAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/career-analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Career Analytics", description = "AI Career Analytics & Progress Intelligence")
@SecurityRequirement(name = "bearerAuth")
public class CareerAnalyticsController {

    private final CareerAnalyticsService careerAnalyticsService;

    @GetMapping
    @Operation(summary = "Get stored Career Analytics Intelligence for authenticated user")
    public ResponseEntity<CareerAnalyticsResponse> getAnalytics(Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("REST: Fetching stored Career Analytics for user: {}", userEmail);
        CareerAnalyticsResponse response = careerAnalyticsService.getAnalytics(userEmail);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate Career Analytics (returns cached if present, else calls AI)")
    public ResponseEntity<CareerAnalyticsResponse> generateAnalytics(Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("REST: Generating Career Analytics for user: {}", userEmail);
        CareerAnalyticsResponse response = careerAnalyticsService.generateAnalytics(userEmail);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/regenerate")
    @Operation(summary = "Force regenerate AI Career Analytics Intelligence via Gemini AI")
    public ResponseEntity<CareerAnalyticsResponse> regenerateAnalytics(Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("REST: Force regenerating AI Career Analytics for user: {}", userEmail);
        CareerAnalyticsResponse response = careerAnalyticsService.regenerateAnalytics(userEmail);
        return ResponseEntity.ok(response);
    }
}
