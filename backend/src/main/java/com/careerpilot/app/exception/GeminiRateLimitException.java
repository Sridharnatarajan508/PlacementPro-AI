package com.careerpilot.app.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Dedicated safe exception for Gemini API 429 / Quota / Rate-limit exhaustion.
 * Contains only safe, sanitized information without exposing API keys, request URLs, or secrets.
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
@Getter
public class GeminiRateLimitException extends RuntimeException {

    private final HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
    private final Integer retryAfterSeconds;

    public GeminiRateLimitException() {
        super("Gemini usage limit reached. Please wait and try again later.");
        this.retryAfterSeconds = null;
    }

    public GeminiRateLimitException(String message) {
        super(message != null && !message.isBlank() ? message : "Gemini usage limit reached. Please wait and try again later.");
        this.retryAfterSeconds = null;
    }

    public GeminiRateLimitException(String message, Integer retryAfterSeconds) {
        super(message != null && !message.isBlank() ? message : "Gemini usage limit reached. Please wait and try again later.");
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
