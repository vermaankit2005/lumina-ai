package com.luminaai.telegram;

import com.luminaai.config.TelegramBotConfig;
import com.luminaai.service.notification.TelegramNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LuminaTelegramBotTest {

    @Mock
    private TelegramBotConfig config;

    @Mock
    private TelegramNotificationService notificationService;

    private LuminaTelegramBot bot;

    @BeforeEach
    void setUp() {
        lenient().when(config.getBotToken()).thenReturn("test-token");
        lenient().when(config.getBotUsername()).thenReturn("TestBot");
        lenient().when(config.getAllowedChatId()).thenReturn(12345L);
        bot = new LuminaTelegramBot(config, notificationService);
    }

    @Test
    void echoesMessageFromAllowedChat() {
        Update update = buildUpdate(12345L, "hello world");
        bot.onUpdateReceived(update);
        verify(notificationService).sendMessage("hello world");
    }

    @Test
    void ignoresMessageFromUnknownChat() {
        Update update = buildUpdate(99999L, "hello world");
        bot.onUpdateReceived(update);
        verifyNoInteractions(notificationService);
    }

    @Test
    void ignoresUpdateWithNoMessage() {
        Update update = new Update();
        bot.onUpdateReceived(update);
        verifyNoInteractions(notificationService);
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
