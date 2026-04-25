package com.luminaai.telegram;

import java.util.Optional;

public record ParsedCommand(Command type, Optional<Integer> taskId) {

    public static ParsedCommand of(Command type) {
        return new ParsedCommand(type, Optional.empty());
    }

    public static ParsedCommand done(int taskId) {
        return new ParsedCommand(Command.DONE, Optional.of(taskId));
    }
}
