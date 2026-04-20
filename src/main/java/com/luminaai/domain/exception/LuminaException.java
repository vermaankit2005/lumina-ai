package com.luminaai.domain.exception;

/**
 * Base runtime exception for all application-level errors in Lumina AI.
 * Subclasses represent specific failure domains (email, notification, analysis).
 */
public class LuminaException extends RuntimeException {

    public LuminaException(String message) {
        super(message);
    }

    public LuminaException(String message, Throwable cause) {
        super(message, cause);
    }
}
