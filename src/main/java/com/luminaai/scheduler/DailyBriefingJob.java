package com.luminaai.scheduler;

import com.luminaai.repository.BriefingRunRepository;
import com.luminaai.service.briefing.BriefingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyBriefingJob {

    private final BriefingService briefingService;
    private final BriefingRunRepository briefingRunRepository;

    @Scheduled(cron = "0 0 7 * * *")
    public void runMorningBriefing() {
        if (briefingRunRepository.existsByRunDate(LocalDate.now())) {
            log.info("Briefing already ran today — skipping scheduled run.");
            return;
        }
        log.info("Starting scheduled morning briefing.");
        briefingService.runDailyBriefing();
    }
}
