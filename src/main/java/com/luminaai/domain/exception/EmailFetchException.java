package com.luminaai.domain.exception;

/**
 * Thrown when an email provider fails to deliver messages (network error, auth failure, etc.).
 */
public class EmailFetchException extends LuminaException {

    public EmailFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
