package com.luminaai.telegram;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CommandParser {

    private static final Pattern DONE_PATTERN = Pattern.compile(
            "^/?done\\s+#?(\\d+)$", Pattern.CASE_INSENSITIVE);

    public ParsedCommand parse(String text) {
        if (text == null || text.isBlank()) {
            return ParsedCommand.of(Command.UNKNOWN);
        }

        String trimmed = text.strip();

        if (trimmed.equalsIgnoreCase("/briefing") || trimmed.equalsIgnoreCase("briefing")) {
            return ParsedCommand.of(Command.BRIEFING);
        }

        if (trimmed.equalsIgnoreCase("/tasks") || trimmed.equalsIgnoreCase("tasks")) {
            return ParsedCommand.of(Command.TASKS);
        }

        if (trimmed.equalsIgnoreCase("/add") || trimmed.equalsIgnoreCase("add")) {
            return ParsedCommand.of(Command.ADD);
        }

        if (trimmed.equalsIgnoreCase("/help") || trimmed.equalsIgnoreCase("help")) {
            return ParsedCommand.of(Command.HELP);
        }

        Matcher doneMatcher = DONE_PATTERN.matcher(trimmed);
        if (doneMatcher.matches()) {
            int taskId = Integer.parseInt(doneMatcher.group(1));
            return ParsedCommand.done(taskId);
        }

        // "done" with no valid integer → DONE with empty taskId
        if (trimmed.toLowerCase().startsWith("done") || trimmed.toLowerCase().startsWith("/done")) {
            return ParsedCommand.of(Command.DONE);
        }

        return ParsedCommand.of(Command.UNKNOWN);
    }
}
