package com.careerpilot.app.controller;

import com.careerpilot.app.dto.request.CareerRoadmapRequest;
import com.careerpilot.app.dto.response.CareerRoadmapResponse;
import com.careerpilot.app.dto.response.LearningRecommendationResponse;
import com.careerpilot.app.dto.response.RoadmapProgressResponse;
import com.careerpilot.app.dto.response.RoadmapTaskResponse;
import com.careerpilot.app.service.CareerRoadmapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/career-roadmap", "/api/career-roadmaps"})
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Career Roadmap", description = "AI Career Roadmap & Personalized Learning Engine")
@SecurityRequirement(name = "bearerAuth")
public class CareerRoadmapController {

    private final CareerRoadmapService careerRoadmapService;

    @GetMapping
    @Operation(summary = "Get latest AI career roadmap")
    public ResponseEntity<CareerRoadmapResponse> getLatestRoadmap(Authentication authentication) {
        String userEmail = authentication.getName();
        CareerRoadmapResponse response = careerRoadmapService.getLatestRoadmap(userEmail);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate AI career roadmap (cached if already exists)")
    public ResponseEntity<CareerRoadmapResponse> generateRoadmap(Authentication authentication) {
        String userEmail = authentication.getName();
        CareerRoadmapResponse response = careerRoadmapService.generateRoadmap(userEmail);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/regenerate")
    @Operation(summary = "Force regenerate AI career roadmap and learning recommendations")
    public ResponseEntity<CareerRoadmapResponse> regenerateRoadmap(Authentication authentication) {
        String userEmail = authentication.getName();
        CareerRoadmapResponse response = careerRoadmapService.regenerateRoadmap(userEmail);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/learning")
    @Operation(summary = "Get personalized learning recommendations")
    public ResponseEntity<List<LearningRecommendationResponse>> getLearningRecommendations(Authentication authentication) {
        String userEmail = authentication.getName();
        List<LearningRecommendationResponse> recs = careerRoadmapService.getLearningRecommendations(userEmail);
        return ResponseEntity.ok(recs);
    }

    // --- Legacy / Multi-Roadmap Helper Endpoints ---

    @GetMapping("/latest")
    @Operation(summary = "Get latest career roadmap (alias)")
    public ResponseEntity<CareerRoadmapResponse> getLatestRoadmapAlias(Authentication authentication) {
        String userEmail = authentication.getName();
        CareerRoadmapResponse response = careerRoadmapService.getLatestRoadmap(userEmail);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Generate career roadmap with custom request body")
    public ResponseEntity<CareerRoadmapResponse> generateRoadmapWithBody(
            @Valid @RequestBody(required = false) CareerRoadmapRequest request,
            Authentication authentication) {
        String userEmail = authentication.getName();
        CareerRoadmapResponse response = careerRoadmapService.generateRoadmap(request, userEmail);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    @Operation(summary = "Get all career roadmaps for user")
    public ResponseEntity<List<CareerRoadmapResponse>> getAllRoadmaps(Authentication authentication) {
        String userEmail = authentication.getName();
        List<CareerRoadmapResponse> list = careerRoadmapService.getRoadmaps(userEmail);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get career roadmap by ID")
    public ResponseEntity<CareerRoadmapResponse> getRoadmapById(
            @PathVariable Long id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        CareerRoadmapResponse response = careerRoadmapService.getRoadmapById(id, userEmail);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a career roadmap by ID")
    public ResponseEntity<Map<String, String>> deleteRoadmap(
            @PathVariable Long id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        careerRoadmapService.deleteRoadmap(id, userEmail);
        return ResponseEntity.ok(Map.of("message", "Career roadmap successfully deleted"));
    }

    @PostMapping("/{roadmapId}/tasks/{taskId}/complete")
    @Operation(summary = "Mark a roadmap task as completed")
    public ResponseEntity<RoadmapTaskResponse> completeTask(
            @PathVariable Long roadmapId,
            @PathVariable Long taskId,
            Authentication authentication) {
        String userEmail = authentication.getName();
        RoadmapTaskResponse response = careerRoadmapService.completeTask(roadmapId, taskId, userEmail);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{roadmapId}/tasks/{taskId}/uncomplete")
    @Operation(summary = "Mark a roadmap task as uncompleted")
    public ResponseEntity<RoadmapTaskResponse> uncompleteTask(
            @PathVariable Long roadmapId,
            @PathVariable Long taskId,
            Authentication authentication) {
        String userEmail = authentication.getName();
        RoadmapTaskResponse response = careerRoadmapService.uncompleteTask(roadmapId, taskId, userEmail);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/progress")
    @Operation(summary = "Get roadmap progress statistics")
    public ResponseEntity<RoadmapProgressResponse> getProgress(
            @PathVariable Long id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        RoadmapProgressResponse response = careerRoadmapService.getProgress(id, userEmail);
        return ResponseEntity.ok(response);
    }
}
