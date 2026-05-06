package com.luminaai.telegram;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DeadlineParserTest {

    private final DeadlineParser parser = new DeadlineParser();

    @Test
    void parsesToday() {
        assertThat(parser.parse("today")).contains(LocalDate.now());
    }

    @Test
    void parsesTomorrow() {
        assertThat(parser.parse("tomorrow")).contains(LocalDate.now().plusDays(1));
    }

    @Test
    void parsesNextWeek() {
        assertThat(parser.parse("next week")).contains(LocalDate.now().plusWeeks(1));
    }

    @Test
    void parsesIsoDate() {
        assertThat(parser.parse("2026-12-25")).contains(LocalDate.of(2026, 12, 25));
    }

    @Test
    void parsesDateWithShortMonth() {
        Optional<LocalDate> result = parser.parse("25 Dec");
        assertThat(result).isPresent();
        assertThat(result.get().getDayOfMonth()).isEqualTo(25);
        assertThat(result.get().getMonthValue()).isEqualTo(12);
    }

    @Test
    void parsesDateWithFullMonth() {
        Optional<LocalDate> result = parser.parse("25 December");
        assertThat(result).isPresent();
        assertThat(result.get().getDayOfMonth()).isEqualTo(25);
    }

    @Test
    void parsesDateWithExplicitYear() {
        assertThat(parser.parse("25 Dec 2027")).contains(LocalDate.of(2027, 12, 25));
    }

    @ParameterizedTest
    @ValueSource(strings = {"garbage", "someday", "asap", ""})
    void returnsEmptyForUnrecognisedInput(String input) {
        assertThat(parser.parse(input)).isEmpty();
    }

    @Test
    void returnsEmptyForNull() {
        assertThat(parser.parse(null)).isEmpty();
    }
}
