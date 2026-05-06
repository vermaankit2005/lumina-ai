package com.luminaai.telegram;

import com.luminaai.config.TelegramBotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
            return;
        }

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

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        if (chatId != config.getAllowedChatId()) {
            log.warn("Ignored callback from unauthorised chat ID: {}", chatId);
            return;
        }

        log.info("Received callback from chat {}: {}", chatId, callbackQuery.getData());
        commandHandler.handleCallback(callbackQuery.getData(), String.valueOf(chatId));

        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQuery.getId());
        try {
            execute(answer);
        } catch (TelegramApiException e) {
            log.warn("Failed to acknowledge callback query: {}", e.getMessage());
        }
    }
}
