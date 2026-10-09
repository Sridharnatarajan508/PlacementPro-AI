package com.careerpilot.app.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalExceptionHandlerRateLimitTest {

    private GlobalExceptionHandler exceptionHandler;
    private MockHttpServletRequest request;

    @BeforeEach
    public void setup() {
        exceptionHandler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/ai-assistant/message");
    }

    @Test
    public void testHandleGeminiRateLimitWithRetryAfter() {
        GeminiRateLimitException ex = new GeminiRateLimitException("Gemini usage limit reached. Please wait and try again later.", 45);
        ResponseEntity<ApiError> response = exceptionHandler.handleGeminiRateLimit(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("45", response.getHeaders().getFirst("Retry-After"));

        ApiError body = response.getBody();
        assertNotNull(body);
        assertEquals(429, body.getStatus());
        assertEquals("GEMINI_RATE_LIMIT", body.getError());
        assertEquals("Gemini usage limit reached. Please wait and try again later.", body.getMessage());
        assertEquals("/api/ai-assistant/message", body.getPath());
        assertEquals(45, body.getRetryAfterSeconds());
        assertNotNull(body.getTimestamp());
    }

    @Test
    public void testHandleGeminiRateLimitWithoutRetryAfter() {
        GeminiRateLimitException ex = new GeminiRateLimitException("Gemini usage limit reached. Please wait and try again later.");
        ResponseEntity<ApiError> response = exceptionHandler.handleGeminiRateLimit(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertNull(response.getHeaders().getFirst("Retry-After"));

        ApiError body = response.getBody();
        assertNotNull(body);
        assertEquals(429, body.getStatus());
        assertEquals("GEMINI_RATE_LIMIT", body.getError());
        assertEquals("Gemini usage limit reached. Please wait and try again later.", body.getMessage());
        assertNull(body.getRetryAfterSeconds());
    }

    @Test
    public void testHandleAIServiceExceptionReturnsBadGateway() {
        AIServiceException ex = new AIServiceException("AI service authentication failed.", HttpStatus.BAD_GATEWAY);
        ResponseEntity<ApiError> response = exceptionHandler.handleAIServiceException(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());

        ApiError body = response.getBody();
        assertNotNull(body);
        assertEquals(502, body.getStatus());
        assertEquals("AI service authentication failed.", body.getMessage());
    }
}
