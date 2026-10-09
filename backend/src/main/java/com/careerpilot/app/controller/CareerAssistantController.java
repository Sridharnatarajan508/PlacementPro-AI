package com.careerpilot.app.controller;

import com.careerpilot.app.dto.AssistantConversationDto;
import com.careerpilot.app.dto.request.CareerAssistantRequest;
import com.careerpilot.app.dto.response.CareerAssistantResponse;
import com.careerpilot.app.dto.response.CareerChatResponse;
import com.careerpilot.app.service.CareerAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CareerAssistantController {

    private final CareerAssistantService careerAssistantService;

    // =========================================================================
    // Phase 10 Primary Endpoints (/api/assistant)
    // =========================================================================

    @PostMapping({"/api/assistant/conversations", "/api/ai-assistant/chats"})
    public ResponseEntity<AssistantConversationDto> createConversation(
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        String userEmail = authentication.getName();
        String title = (body != null && body.containsKey("title")) ? body.get("title") : "New Conversation";
        log.info("REST: Create new assistant conversation for user: {} with title: {}", userEmail, title);
        AssistantConversationDto conv = careerAssistantService.createConversation(userEmail, title);
        return ResponseEntity.status(HttpStatus.CREATED).body(conv);
    }

    @GetMapping({"/api/assistant/conversations", "/api/ai-assistant/chats"})
    public ResponseEntity<List<AssistantConversationDto>> getConversations(Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("REST: Fetch all assistant conversations for user: {}", userEmail);
        List<AssistantConversationDto> convs = careerAssistantService.getConversations(userEmail);
        return ResponseEntity.ok(convs);
    }

    @GetMapping({"/api/assistant/conversations/{id}", "/api/ai-assistant/chats/{id}"})
    public ResponseEntity<AssistantConversationDto> getConversation(
            @PathVariable("id") Long id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("REST: Fetch assistant conversation ID {} for user: {}", id, userEmail);
        AssistantConversationDto conv = careerAssistantService.getConversation(id, userEmail);
        return ResponseEntity.ok(conv);
    }

    @PostMapping("/api/assistant/conversations/{id}/messages")
    public ResponseEntity<CareerAssistantResponse> sendMessageToConversation(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String userEmail = authentication.getName();
        String message = (body != null) ? body.get("message") : "";
        log.info("REST: Send message to assistant conversation ID {} for user: {}", id, userEmail);
        CareerAssistantResponse response = careerAssistantService.sendMessage(userEmail, id, message);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping({"/api/assistant/conversations/{id}", "/api/ai-assistant/chats/{id}"})
    public ResponseEntity<Map<String, String>> deleteConversation(
            @PathVariable("id") Long id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("REST: Delete assistant conversation ID {} for user: {}", id, userEmail);
        careerAssistantService.deleteConversation(id, userEmail);
        return ResponseEntity.ok(Map.of("message", "Conversation deleted successfully"));
    }

    @PostMapping("/api/assistant/ask")
    public ResponseEntity<CareerAssistantResponse> askQuickQuestion(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String userEmail = authentication.getName();
        String message = (body != null) ? body.get("message") : "";
        log.info("REST: Quick career question for user: {}", userEmail);
        CareerAssistantResponse response = careerAssistantService.askQuickQuestion(userEmail, message);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // Backwards Compatibility Endpoints
    // =========================================================================

    @PostMapping("/api/ai-assistant/message")
    public ResponseEntity<CareerAssistantResponse> sendMessageLegacy(
            @Valid @RequestBody CareerAssistantRequest request,
            Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("REST: Legacy send message to Career Assistant for user: {} (chatId: {})", userEmail, request.getChatId());
        CareerAssistantResponse response = careerAssistantService.sendMessage(userEmail, request);
        return ResponseEntity.ok(response);
    }
}
