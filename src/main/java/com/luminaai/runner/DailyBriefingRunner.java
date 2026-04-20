package com.luminaai.runner;

import com.luminaai.service.briefing.BriefingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Application entry point that triggers the daily email briefing pipeline on startup.
 * Delegates all logic to {@link BriefingService} to keep the runner thin and testable.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyBriefingRunner implements CommandLineRunner {

    private final BriefingService briefingService;

    @Override
    public void run(String... args) {
        log.info("Lumina AI starting — launching daily briefing pipeline.");
        briefingService.runDailyBriefing();
    }
}
