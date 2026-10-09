package com.careerpilot.app.exception;

/**
 * Thrown when an uploaded file fails validation (wrong type, too large, empty, corrupted, etc.).
 */
public class FileValidationException extends RuntimeException {
    public FileValidationException(String message) {
        super(message);
    }
}
