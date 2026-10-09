package com.careerpilot.app.controller;

import com.careerpilot.app.dto.request.InterviewAnswerRequest;
import com.careerpilot.app.dto.request.InterviewStartRequest;
import com.careerpilot.app.dto.response.*;
import com.careerpilot.app.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
@Slf4j
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping("/start")
    public ResponseEntity<InterviewSessionResponse> startInterview(
            @Valid @RequestBody(required = false) InterviewStartRequest request) {
        String email = getAuthenticatedEmail();
        log.info("REST: Start mock interview request from user: {}", email);
        InterviewSessionResponse response = interviewService.startInterview(
                request != null ? request : new InterviewStartRequest(), email);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{sessionId}/questions/{questionId}/answer")
    public ResponseEntity<InterviewEvaluationResponse> submitAnswer(
            @PathVariable Long sessionId,
            @PathVariable Long questionId,
            @Valid @RequestBody InterviewAnswerRequest request) {
        String email = getAuthenticatedEmail();
        log.info("REST: User {} submitting answer for session ID {}, question ID {}", email, sessionId, questionId);
        InterviewEvaluationResponse response = interviewService.submitAnswer(
                sessionId, questionId, request.getAnswer(), email);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<InterviewSessionResponse> getInterview(
            @PathVariable Long sessionId) {
        String email = getAuthenticatedEmail();
        log.info("REST: Fetch interview session ID {} for user: {}", sessionId, email);
        return ResponseEntity.ok(interviewService.getInterview(sessionId, email));
    }

    @GetMapping("/history")
    public ResponseEntity<List<InterviewHistoryResponse>> getInterviewHistory() {
        String email = getAuthenticatedEmail();
        log.info("REST: Fetch interview history for user: {}", email);
        return ResponseEntity.ok(interviewService.getInterviewHistory(email));
    }

    @GetMapping("/{sessionId}/next-question")
    public ResponseEntity<InterviewQuestionResponse> getNextQuestion(
            @PathVariable Long sessionId) {
        String email = getAuthenticatedEmail();
        log.info("REST: Fetch next active question for session ID {}, user: {}", sessionId, email);
        return ResponseEntity.ok(interviewService.getNextQuestion(sessionId, email));
    }

    @PostMapping("/{sessionId}/abandon")
    public ResponseEntity<Void> abandonInterview(
            @PathVariable Long sessionId) {
        String email = getAuthenticatedEmail();
        log.info("REST: User {} abandoning interview session ID {}", email, sessionId);
        interviewService.abandonInterview(sessionId, email);
        return ResponseEntity.noContent().build();
    }

    private String getAuthenticatedEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
