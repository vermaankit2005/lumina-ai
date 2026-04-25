package com.luminaai.service.notification;

import com.luminaai.config.TelegramBotConfig;
import com.luminaai.domain.exception.NotificationException;
import com.luminaai.port.NotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * {@link NotificationPort} implementation that delivers messages to a configured
 * Telegram chat using the Bot API.
 *
 * <p>If the bot token is absent (e.g. local development without Telegram), the send
 * is silently skipped rather than throwing, so the rest of the pipeline is unaffected.
 */
@Slf4j
@Service
public class TelegramNotificationService extends DefaultAbsSender implements NotificationPort {

    private final TelegramBotConfig config;

    public TelegramNotificationService(TelegramBotConfig config) {
        super(new DefaultBotOptions(), config.getBotToken());
        this.config = config;
    }

    @Override
    public void send(String message) {
        if (config.getBotToken() == null || config.getBotToken().isBlank()) {
            log.warn("Telegram bot token is not configured — skipping notification.");
            return;
        }

        SendMessage request = new SendMessage();
        request.setChatId(String.valueOf(config.getAllowedChatId()));
        request.setText(message);
        request.setParseMode("Markdown");

        try {
            execute(request);
            log.info("Telegram message delivered to chat {}", config.getAllowedChatId());
        } catch (TelegramApiException e) {
            log.error("Failed to deliver Telegram message", e);
            throw new NotificationException("Telegram delivery failed", e);
        }
    }

    @Override
    public String getBotToken() {
        return config.getBotToken();
    }
}
