package com.luminaai.service.notification;

import com.luminaai.config.TelegramBotConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramNotificationServiceTest {

    @Mock
    private TelegramBotConfig config;

    private TelegramNotificationService service;

    @BeforeEach
    void setUp() {
        when(config.getBotToken()).thenReturn("");
        service = new TelegramNotificationService(config);
    }

    @Test
    void send_doesNotThrowWhenTokenIsEmpty() {
        // When the bot token is absent (local dev without Telegram), send() must
        // log a warning and return gracefully rather than throwing.
        assertDoesNotThrow(() -> service.send("hello"));
    }
}
