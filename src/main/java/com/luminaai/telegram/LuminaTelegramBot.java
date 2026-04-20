package com.luminaai.telegram;

import com.luminaai.config.TelegramBotConfig;
import com.luminaai.port.NotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Telegram long-polling bot that receives inbound messages and echoes them back
 * via the {@link NotificationPort}.
 *
 * <p>Messages originating from any chat other than the configured
 * {@code lumina.telegram.allowed-chat-id} are silently ignored.
 */
@Slf4j
@Component
public class LuminaTelegramBot extends TelegramLongPollingBot {

    private final TelegramBotConfig config;
    private final NotificationPort notificationPort;

    public LuminaTelegramBot(TelegramBotConfig config, NotificationPort notificationPort) {
        super(config.getBotToken());
        this.config = config;
        this.notificationPort = notificationPort;
    }

    @Override
    public String getBotUsername() {
        return config.getBotUsername();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.getMessage() == null || update.getMessage().getText() == null) {
            return;
        }

        long chatId = update.getMessage().getChatId();
        if (chatId != config.getAllowedChatId()) {
            log.warn("Ignored message from unauthorised chat ID: {}", chatId);
            return;
        }

        String text = update.getMessage().getText();
        log.info("Received message from chat {}: {}", chatId, text);
        notificationPort.send(text);
    }
}
