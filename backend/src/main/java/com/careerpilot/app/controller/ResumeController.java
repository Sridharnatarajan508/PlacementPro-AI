package com.careerpilot.app.controller;

import com.careerpilot.app.dto.response.ResumeResponse;
import com.careerpilot.app.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
@Tag(name = "Resume", description = "Resume management and PDFBox text extraction APIs")
@SecurityRequirement(name = "bearerAuth")
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload PDF resume and extract text using Apache PDFBox")
    public ResponseEntity<ResumeResponse> uploadResume(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        ResumeResponse response = resumeService.uploadResume(userDetails.getUsername(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all uploaded resumes for authenticated user")
    public ResponseEntity<List<ResumeResponse>> getAllResumes(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ResumeResponse> resumes = resumeService.getAllResumesForUser(userDetails.getUsername());
        return ResponseEntity.ok(resumes);
    }

    @GetMapping("/latest")
    @Operation(summary = "Get latest uploaded resume for authenticated user")
    public ResponseEntity<ResumeResponse> getLatestResume(
            @AuthenticationPrincipal UserDetails userDetails) {
        ResumeResponse resume = resumeService.getLatestResumeForUser(userDetails.getUsername());
        return ResponseEntity.ok(resume);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get specific resume by ID for authenticated user")
    public ResponseEntity<ResumeResponse> getResumeById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        ResumeResponse resume = resumeService.getResumeById(userDetails.getUsername(), id);
        return ResponseEntity.ok(resume);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete resume by ID for authenticated user")
    public ResponseEntity<Void> deleteResume(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        resumeService.deleteResume(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    // --- Phase 3: Gemini AI Resume Analysis Endpoints ---

    private final com.careerpilot.app.service.ResumeAnalysisService resumeAnalysisService;

    @PostMapping("/{id}/analyze")
    @Operation(summary = "Perform or re-analyze resume using Google Gemini AI")
    public ResponseEntity<com.careerpilot.app.dto.response.ResumeAnalysisResponse> analyzeResume(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        com.careerpilot.app.dto.response.ResumeAnalysisResponse analysis =
                resumeAnalysisService.analyzeResume(userDetails.getUsername(), id);
        return ResponseEntity.ok(analysis);
    }

    @GetMapping("/{id}/analysis")
    @Operation(summary = "Get stored Gemini AI analysis for resume")
    public ResponseEntity<com.careerpilot.app.dto.response.ResumeAnalysisResponse> getResumeAnalysis(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        com.careerpilot.app.dto.response.ResumeAnalysisResponse analysis =
                resumeAnalysisService.getAnalysisForResume(userDetails.getUsername(), id);
        return ResponseEntity.ok(analysis);
    }

    @DeleteMapping("/{id}/analysis")
    @Operation(summary = "Delete stored AI analysis for resume")
    public ResponseEntity<Void> deleteResumeAnalysis(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        resumeAnalysisService.deleteAnalysisForResume(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    private final com.careerpilot.app.service.GeminiService geminiService;

    @PostMapping("/test-gemini")
    @Operation(summary = "Diagnostic test: Send simple GEMINI_OK prompt to Gemini API")
    public ResponseEntity<java.util.Map<String, String>> testGemini() {
        String response = geminiService.testSimpleGeminiCall();
        return ResponseEntity.ok(java.util.Map.of("status", "SUCCESS", "reply", response));
    }
}
