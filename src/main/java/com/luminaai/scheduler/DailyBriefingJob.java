package com.luminaai.scheduler;

import com.luminaai.domain.enums.RunStatus;
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
        if (briefingRunRepository.findByRunDateAndStatus(LocalDate.now(), RunStatus.SUCCESS).isPresent()) {
            log.info("Briefing already succeeded today — skipping scheduled run.");
            return;
        }
        log.info("Starting scheduled morning briefing.");
        briefingService.runDailyBriefing();
    }
}
