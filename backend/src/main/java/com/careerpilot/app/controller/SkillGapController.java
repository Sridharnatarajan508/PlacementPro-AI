package com.careerpilot.app.controller;

import com.careerpilot.app.dto.request.SkillGapAnalyzeRequest;
import com.careerpilot.app.dto.response.SkillGapAnalysisResponse;
import com.careerpilot.app.service.SkillGapService;
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
@RequestMapping("/api/skill-gaps")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Skill Gaps", description = "AI Skill Gap Analysis & Learning Recommendations")
@SecurityRequirement(name = "bearerAuth")
public class SkillGapController {

    private final SkillGapService skillGapService;

    @PostMapping("/analyze")
    @Operation(summary = "Analyze skill gaps against target role or job")
    public ResponseEntity<SkillGapAnalysisResponse> analyzeSkillGap(
            @RequestBody(required = false) SkillGapAnalyzeRequest request,
            Authentication authentication) {
        String userEmail = authentication.getName();
        if (request == null) {
            request = new SkillGapAnalyzeRequest();
        }
        SkillGapAnalysisResponse response = skillGapService.analyzeSkillGap(request, userEmail);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/latest")
    @Operation(summary = "Get latest skill gap analysis for authenticated user")
    public ResponseEntity<SkillGapAnalysisResponse> getLatestSkillGap(Authentication authentication) {
        String userEmail = authentication.getName();
        SkillGapAnalysisResponse response = skillGapService.getLatestSkillGap(userEmail);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    @Operation(summary = "Get historical skill gap analyses for authenticated user")
    public ResponseEntity<List<SkillGapAnalysisResponse>> getSkillGapHistory(Authentication authentication) {
        String userEmail = authentication.getName();
        List<SkillGapAnalysisResponse> history = skillGapService.getSkillGapHistory(userEmail);
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a skill gap analysis by ID")
    public ResponseEntity<Map<String, String>> deleteSkillGap(
            @PathVariable Long id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        skillGapService.deleteSkillGap(id, userEmail);
        return ResponseEntity.ok(Map.of("message", "Skill gap analysis successfully deleted"));
    }
}
