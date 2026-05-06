package com.luminaai.telegram;

import com.luminaai.config.TelegramBotConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LuminaTelegramBotTest {

    @Mock private TelegramBotConfig config;
    @Mock private CommandParser commandParser;
    @Mock private TelegramCommandHandler commandHandler;

    private LuminaTelegramBot bot;

    @BeforeEach
    void setUp() {
        lenient().when(config.getBotToken()).thenReturn("test-token");
        lenient().when(config.getBotUsername()).thenReturn("TestBot");
        lenient().when(config.getAllowedChatId()).thenReturn(12345L);
        bot = new LuminaTelegramBot(config, commandParser, commandHandler);
    }

    @Test
    void routesMessageFromAllowedChatToCommandHandler() {
        ParsedCommand parsed = ParsedCommand.of(Command.BRIEFING);
        when(commandParser.parse("/briefing")).thenReturn(parsed);

        Update update = buildUpdate(12345L, "/briefing");
        bot.onUpdateReceived(update);

        verify(commandParser).parse("/briefing");
        verify(commandHandler).handle(eq(parsed), eq("12345"));
    }

    @Test
    void ignoresMessageFromUnauthorisedChat() {
        Update update = buildUpdate(99999L, "/briefing");
        bot.onUpdateReceived(update);

        verifyNoInteractions(commandParser, commandHandler);
    }

    @Test
    void ignoresUpdateWithNoMessage() {
        bot.onUpdateReceived(new Update());
        verifyNoInteractions(commandParser, commandHandler);
    }

    @Test
    void ignoresUpdateWithNoText() {
        Update update = new Update();
        Message message = new Message();
        Chat chat = new Chat();
        chat.setId(12345L);
        chat.setType("private");
        message.setChat(chat);
        update.setMessage(message);

        bot.onUpdateReceived(update);
        verifyNoInteractions(commandParser, commandHandler);
    }

    @Test
    void routesCallbackQueryFromAllowedChatToCommandHandler() {
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("cbq-1");
        callbackQuery.setData("done:5");
        Message cbMessage = new Message();
        Chat cbChat = new Chat();
        cbChat.setId(12345L);
        cbChat.setType("private");
        cbMessage.setChat(cbChat);
        callbackQuery.setMessage(cbMessage);

        Update update = new Update();
        update.setCallbackQuery(callbackQuery);

        bot.onUpdateReceived(update);

        verify(commandHandler).handleCallback("done:5", "12345");
        verifyNoInteractions(commandParser);
    }

    @Test
    void ignoresCallbackQueryFromUnauthorisedChat() {
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("cbq-2");
        callbackQuery.setData("done:5");
        Message cbMessage = new Message();
        Chat cbChat = new Chat();
        cbChat.setId(99999L);
        cbChat.setType("private");
        cbMessage.setChat(cbChat);
        callbackQuery.setMessage(cbMessage);

        Update update = new Update();
        update.setCallbackQuery(callbackQuery);

        bot.onUpdateReceived(update);

        verifyNoInteractions(commandParser, commandHandler);
    }

    private Update buildUpdate(long chatId, String text) {
        User user = new User();
        user.setId(1L);
        user.setFirstName("Test");
        user.setIsBot(false);

        Chat chat = new Chat();
        chat.setId(chatId);
        chat.setType("private");

        Message message = new Message();
        message.setChat(chat);
        message.setFrom(user);
        message.setText(text);

        Update update = new Update();
        update.setMessage(message);
        return update;
    }
}
