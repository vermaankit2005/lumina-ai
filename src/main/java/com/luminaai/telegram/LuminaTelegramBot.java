package com.luminaai.telegram;

import com.luminaai.config.TelegramBotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Telegram long-polling bot. Inbound updates are logged and authorised against
 * the configured {@code lumina.telegram.allowed-chat-id}; command handling
 * (e.g. {@code /tasks}, {@code /done}) is intentionally not implemented yet.
 */
@Slf4j
@Component
public class LuminaTelegramBot extends TelegramLongPollingBot {

    private final TelegramBotConfig config;

    public LuminaTelegramBot(TelegramBotConfig config) {
        super(config.getBotToken());
        this.config = config;
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

        log.info("Received message from chat {}: {}", chatId, update.getMessage().getText());
    }
}
