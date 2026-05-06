package com.luminaai.telegram;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class DeadlineParser {

    private static final List<String> YEAR_PATTERNS = List.of(
            "d MMM yyyy", "d MMMM yyyy"
    );
    private static final List<String> MONTHDAY_PATTERNS = List.of(
            "d MMM", "d MMMM", "MMM d", "MMMM d"
    );

    public Optional<LocalDate> parse(String input) {
        if (input == null || input.isBlank()) return Optional.empty();
        String trimmed = input.strip().toLowerCase(Locale.ENGLISH);

        if (trimmed.equals("today"))      return Optional.of(LocalDate.now());
        if (trimmed.equals("tomorrow"))   return Optional.of(LocalDate.now().plusDays(1));
        if (trimmed.equals("next week"))  return Optional.of(LocalDate.now().plusWeeks(1));
        if (trimmed.equals("next month")) return Optional.of(LocalDate.now().plusMonths(1));

        // ISO: 2026-05-15
        try {
            return Optional.of(LocalDate.parse(trimmed));
        } catch (DateTimeParseException ignored) {}

        // Patterns with explicit year
        for (String pattern : YEAR_PATTERNS) {
            try {
                LocalDate date = LocalDate.parse(input.strip(),
                        DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH));
                return Optional.of(date);
            } catch (DateTimeParseException ignored) {}
        }

        // Patterns without year — pick current year, roll to next if already past
        for (String pattern : MONTHDAY_PATTERNS) {
            try {
                MonthDay md = MonthDay.parse(input.strip(),
                        DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH));
                LocalDate date = md.atYear(LocalDate.now().getYear());
                if (date.isBefore(LocalDate.now())) {
                    date = date.withYear(date.getYear() + 1);
                }
                return Optional.of(date);
            } catch (DateTimeParseException ignored) {}
        }

        return Optional.empty();
    }
}
