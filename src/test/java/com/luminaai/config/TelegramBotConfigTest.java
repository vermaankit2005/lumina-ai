package com.luminaai.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "lumina.telegram.bot-token=test-token",
    "lumina.telegram.bot-username=TestBot",
    "lumina.telegram.allowed-chat-id=12345"
})
class TelegramBotConfigTest {

    @Autowired
    private TelegramBotConfig config;

    @Test
    void readsTokenFromProperties() {
        assertEquals("test-token", config.getBotToken());
    }

    @Test
    void readsUsernameFromProperties() {
        assertEquals("TestBot", config.getBotUsername());
    }

    @Test
    void readsAllowedChatIdFromProperties() {
        assertEquals(12345L, config.getAllowedChatId());
    }
}
