package com.luminaai.telegram;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConversationState {
    private ConversationStep step;
    private String title;

    public ConversationState() {
        this.step = ConversationStep.AWAITING_TITLE;
    }
}
