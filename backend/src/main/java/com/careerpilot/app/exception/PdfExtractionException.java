package com.careerpilot.app.exception;

/**
 * Thrown when PDF text extraction fails or produces no usable content.
 */
public class PdfExtractionException extends RuntimeException {
    public PdfExtractionException(String message) {
        super(message);
    }

    public PdfExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
