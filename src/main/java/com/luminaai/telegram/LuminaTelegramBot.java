package com.luminaai.telegram;

import com.luminaai.config.TelegramBotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Component
public class LuminaTelegramBot extends TelegramLongPollingBot {

    private final TelegramBotConfig config;
    private final CommandParser commandParser;
    private final TelegramCommandHandler commandHandler;

    public LuminaTelegramBot(TelegramBotConfig config,
                             CommandParser commandParser,
                             TelegramCommandHandler commandHandler) {
        super(config.getBotToken());
        this.config = config;
        this.commandParser = commandParser;
        this.commandHandler = commandHandler;
    }

    @Override
    public String getBotUsername() {
        return config.getBotUsername();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        long chatId = update.getMessage().getChatId();
        if (chatId != config.getAllowedChatId()) {
            log.warn("Ignored message from unauthorised chat ID: {}", chatId);
            return;
        }

        String text = update.getMessage().getText();
        log.info("Received message from chat {}: {}", chatId, text);
        ParsedCommand command = commandParser.parse(text);
        commandHandler.handle(command, String.valueOf(chatId));
    }
}
