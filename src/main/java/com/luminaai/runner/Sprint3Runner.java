package com.luminaai.runner;

import com.luminaai.domain.enums.RunStatus;
import com.luminaai.entity.ActionTask;
import com.luminaai.entity.BriefingRun;
import com.luminaai.repository.ActionTaskRepository;
import com.luminaai.repository.BriefingRunRepository;
import com.luminaai.service.gmail.GmailFetchService;
import com.luminaai.service.notification.TelegramNotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import java.time.LocalDate;
import java.time.LocalDateTime;

@RequiredArgsConstructor
public class Sprint3Runner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Sprint3Runner.class);

    private final GmailFetchService gmailFetchService;
    private final TelegramNotificationService telegramNotificationService;
    private final BriefingRunRepository briefingRunRepository;
    private final ActionTaskRepository actionTaskRepository;

    @Override
    public void run(String... args) {
        log.info("Sprint 3: starting briefing run with persistence...");

        // 1. Record the start of the run
        BriefingRun run = BriefingRun.builder()
                .runDate(LocalDate.now())
                .status(RunStatus.RUNNING)
                .startedAt(LocalDateTime.now())
                .build();
        run = briefingRunRepository.save(run);

        try {
            // 2. Fetch the latest email subject (using existing Sprint 2 logic for now)
            // Note: Sprint 2's fetchLatestEmailSubject doesn't return the ID,
            // but we'll use a simplified check for this sprint by using the subject as a source ID.
            String subject = gmailFetchService.fetchLatestEmailSubject();

            if (subject != null) {
                // Deduplication check: Has this email subject been processed before?
                if (!actionTaskRepository.existsBySourceEmailId(subject)) {
                    telegramNotificationService.sendMessage("New email: " + subject);

                    // Mark as "processed" by saving a task record
                    ActionTask task = ActionTask.builder()
                            .title("Process: " + subject)
                            .sourceEmailId(subject)
                            .briefingRun(run)
                            .build();
                    actionTaskRepository.save(task);

                    run.setEmailsFetched(1);
                    run.setTasksExtracted(1);
                    log.info("New email processed and recorded: {}", subject);
                } else {
                    log.info("Email already processed: {}", subject);
                    telegramNotificationService.sendMessage("No new emails since last run.");
                }
            } else {
                log.info("No unread emails found.");
            }

            // 3. Mark run as success
            run.setStatus(RunStatus.SUCCESS);
            run.setCompletedAt(LocalDateTime.now());
            briefingRunRepository.save(run);
            log.info("Sprint 3 briefing run complete.");

        } catch (Exception e) {
            log.error("Sprint 3 run failed", e);
            run.setStatus(RunStatus.FAILED);
            run.setErrorMessage(e.getMessage());
            run.setCompletedAt(LocalDateTime.now());
            briefingRunRepository.save(run);
        }
    }
}
