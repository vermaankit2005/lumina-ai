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
    void sendMessageDoesNotThrowWhenTokenIsEmpty() {
        // When bot token is empty (no Telegram configured), sendMessage should
        // log a warning and return rather than throwing an exception.
        assertDoesNotThrow(() -> service.sendMessage("hello"));
    }
}
