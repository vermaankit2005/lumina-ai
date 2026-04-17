package com.luminaai.config;

import com.luminaai.telegram.LuminaTelegramBot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class TelegramBotsConfigTest {

    @Mock
    private LuminaTelegramBot bot;

    @Test
    void canInstantiateConfig() {
        // Verifies that TelegramBotsConfig can be instantiated without error.
        // Full registration test would require a live Telegram connection.
        assertDoesNotThrow(() -> new TelegramBotsConfig(bot));
    }
}
