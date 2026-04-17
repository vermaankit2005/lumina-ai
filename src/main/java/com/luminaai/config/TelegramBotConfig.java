package com.luminaai.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class TelegramBotConfig {

    @Value("${lumina.telegram.bot-token}")
    private String botToken;

    @Value("${lumina.telegram.bot-username}")
    private String botUsername;

    @Value("${lumina.telegram.allowed-chat-id}")
    private long allowedChatId;
}
