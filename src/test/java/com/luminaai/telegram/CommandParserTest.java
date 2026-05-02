package com.luminaai.telegram;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CommandParserTest {

    private final CommandParser parser = new CommandParser();

    @ParameterizedTest
    @ValueSource(strings = {"/briefing", "/BRIEFING", "briefing", "BRIEFING"})
    void parsesBriefingCommand(String input) {
        ParsedCommand result = parser.parse(input);
        assertThat(result.type()).isEqualTo(Command.BRIEFING);
        assertThat(result.taskId()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/tasks", "/TASKS", "tasks", "TASKS"})
    void parsesTasksCommand(String input) {
        ParsedCommand result = parser.parse(input);
        assertThat(result.type()).isEqualTo(Command.TASKS);
        assertThat(result.taskId()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"done #3", "done 3", "DONE #3", "DONE 3", "/done #3", "/done 3"})
    void parsesDoneCommandWithTaskId(String input) {
        ParsedCommand result = parser.parse(input);
        assertThat(result.type()).isEqualTo(Command.DONE);
        assertThat(result.taskId()).contains(3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"done abc", "done #abc", "done"})
    void parsesDoneWithNoValidIdAsEmptyTaskId(String input) {
        ParsedCommand result = parser.parse(input);
        assertThat(result.type()).isEqualTo(Command.DONE);
        assertThat(result.taskId()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/help", "/HELP", "help", "HELP"})
    void parsesHelpCommand(String input) {
        ParsedCommand result = parser.parse(input);
        assertThat(result.type()).isEqualTo(Command.HELP);
        assertThat(result.taskId()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"hello", "random text", "123"})
    void parsesUnknownCommands(String input) {
        ParsedCommand result = parser.parse(input);
        assertThat(result.type()).isEqualTo(Command.UNKNOWN);
    }

    @Test
    void parsesNullAsUnknown() {
        ParsedCommand result = parser.parse(null);
        assertThat(result.type()).isEqualTo(Command.UNKNOWN);
    }

    @Test
    void parsesEmptyStringAsUnknown() {
        ParsedCommand result = parser.parse("   ");
        assertThat(result.type()).isEqualTo(Command.UNKNOWN);
    }
}
