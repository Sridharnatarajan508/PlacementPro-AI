package com.careerpilot.app.controller;

import com.careerpilot.app.exception.AIServiceException;
import com.careerpilot.app.service.GeminiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Diagnostic", description = "Diagnostic APIs for Gemini AI Integration")
@SecurityRequirement(name = "bearerAuth")
public class AiDiagnosticController {

    private final GeminiService geminiService;

    @GetMapping("/diagnostic")
    @Operation(summary = "Perform simple GEMINI_OK diagnostic check")
    public ResponseEntity<Map<String, Object>> runDiagnostic() {
        Map<String, Object> result = new HashMap<>();
        String modelName = geminiService.getSanitizedModelName();
        result.put("model", modelName);

        try {
            String reply = geminiService.testSimpleGeminiCall();
            result.put("success", true);
            result.put("message", reply);
            return ResponseEntity.ok(result);
        } catch (AIServiceException e) {
            log.error("AI Diagnostic test failed: {}", e.getMessage());
            result.put("success", false);
            result.put("status", 400); // Default client/service status
            result.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.OK).body(result); // Return 200 with success=false for diagnostic payload
        } catch (Exception e) {
            log.error("AI Diagnostic test unexpected error: {}", e.getMessage());
            result.put("success", false);
            result.put("status", 500);
            result.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.OK).body(result);
        }
    }
}
