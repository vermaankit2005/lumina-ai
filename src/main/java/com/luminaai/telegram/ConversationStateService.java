package com.luminaai.telegram;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationStateService {

    private final Map<Long, ConversationState> states = new ConcurrentHashMap<>();

    public void start(long chatId) {
        states.put(chatId, new ConversationState());
    }

    public Optional<ConversationState> get(long chatId) {
        return Optional.ofNullable(states.get(chatId));
    }

    public void clear(long chatId) {
        states.remove(chatId);
    }

    public boolean isActive(long chatId) {
        return states.containsKey(chatId);
    }
}
