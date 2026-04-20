package com.luminaai.domain.exception;

/**
 * Thrown when a notification cannot be delivered (e.g. Telegram API failure).
 */
public class NotificationException extends LuminaException {

    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
