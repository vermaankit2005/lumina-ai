package com.luminaai.service.notification;

import com.luminaai.config.TelegramBotConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
public class TelegramNotificationService extends DefaultAbsSender {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);

    private final TelegramBotConfig config;

    public TelegramNotificationService(TelegramBotConfig config) {
        super(new DefaultBotOptions(), config.getBotToken());
        this.config = config;
    }

    public void sendMessage(String text) {
        if (config.getBotToken() == null || config.getBotToken().isBlank()) {
            log.warn("Telegram bot token is not configured. Skipping message send.");
            return;
        }
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(config.getAllowedChatId()));
        message.setText(text);
        try {
            execute(message);
            log.info("Telegram message sent to chat {}", config.getAllowedChatId());
        } catch (TelegramApiException e) {
            log.error("Failed to send Telegram message", e);
        }
    }

    @Override
    public String getBotToken() {
        return config.getBotToken();
    }
}
