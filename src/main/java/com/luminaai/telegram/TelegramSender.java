package com.luminaai.telegram;

import com.luminaai.port.NotificationPort;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public interface TelegramSender extends NotificationPort {
    void sendWithKeyboard(String message, InlineKeyboardMarkup keyboard);
}
