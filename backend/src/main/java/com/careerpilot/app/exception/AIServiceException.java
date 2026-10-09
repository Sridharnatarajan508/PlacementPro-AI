package com.careerpilot.app.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an AI operation fails (e.g. Gemini API error, rate limit, missing key, invalid JSON response).
 */
public class AIServiceException extends RuntimeException {
    private final HttpStatus httpStatus;

    public AIServiceException(String message) {
        super(message);
        this.httpStatus = HttpStatus.BAD_GATEWAY;
    }

    public AIServiceException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public AIServiceException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = HttpStatus.BAD_GATEWAY;
    }

    public AIServiceException(String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus != null ? httpStatus : HttpStatus.BAD_GATEWAY;
    }
}
