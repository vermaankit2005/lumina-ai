package com.luminaai.telegram;

import com.luminaai.config.TelegramBotConfig;
import com.luminaai.service.notification.TelegramNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class LuminaTelegramBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(LuminaTelegramBot.class);

    private final TelegramBotConfig config;
    private final TelegramNotificationService notificationService;

    public LuminaTelegramBot(TelegramBotConfig config, TelegramNotificationService notificationService) {
        super(config.getBotToken());
        this.config = config;
        this.notificationService = notificationService;
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
            log.warn("Ignored message from unknown chat ID: {}", chatId);
            return;
        }
        String text = update.getMessage().getText();
        log.info("Received message from chat {}: {}", chatId, text);
        notificationService.sendMessage(text);
    }
}
