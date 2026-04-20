package com.luminaai.port;

/**
 * Port that abstracts the notification delivery channel.
 * Implementations may deliver via Telegram, Slack, email, or any other medium.
 */
public interface NotificationPort {
    void send(String message);
}
