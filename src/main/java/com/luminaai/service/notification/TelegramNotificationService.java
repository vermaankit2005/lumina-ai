package com.luminaai.service.notification;

import com.luminaai.config.TelegramBotConfig;
import com.luminaai.domain.exception.NotificationException;
import com.luminaai.port.NotificationPort;
import com.luminaai.telegram.TelegramSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Service
public class TelegramNotificationService extends DefaultAbsSender implements TelegramSender {

    private final TelegramBotConfig config;

    public TelegramNotificationService(TelegramBotConfig config) {
        super(new DefaultBotOptions(), config.getBotToken());
        this.config = config;
    }

    @Override
    public void send(String message) {
        sendMessage(buildMessage(message, null));
    }

    @Override
    public void sendWithKeyboard(String message, InlineKeyboardMarkup keyboard) {
        sendMessage(buildMessage(message, keyboard));
    }

    private SendMessage buildMessage(String text, InlineKeyboardMarkup keyboard) {
        SendMessage request = new SendMessage();
        request.setChatId(String.valueOf(config.getAllowedChatId()));
        request.setText(text);
        request.setParseMode("Markdown");
        if (keyboard != null) {
            request.setReplyMarkup(keyboard);
        }
        return request;
    }

    private void sendMessage(SendMessage request) {
        if (config.getBotToken() == null || config.getBotToken().isBlank()) {
            log.warn("Telegram bot token is not configured — skipping notification.");
            return;
        }
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

