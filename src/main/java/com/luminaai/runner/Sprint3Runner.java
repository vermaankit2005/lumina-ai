package com.luminaai.runner;

import com.luminaai.domain.enums.RunStatus;
import com.luminaai.domain.model.EmailMessage;
import com.luminaai.entity.BriefingRun;
import com.luminaai.entity.ProcessedEmail;
import com.luminaai.repository.BriefingRunRepository;
import com.luminaai.repository.ProcessedEmailRepository;
import com.luminaai.service.gmail.EmailFetcher;
import com.luminaai.service.notification.TelegramNotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class Sprint3Runner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Sprint3Runner.class);

    private final EmailFetcher emailFetcher;
    private final TelegramNotificationService telegramNotificationService;
    private final BriefingRunRepository briefingRunRepository;
    private final ProcessedEmailRepository processedEmailRepository;

    @Override
    public void run(String... args) {
        log.info("Sprint 3: starting briefing run with persistence...");

        BriefingRun run = BriefingRun.builder()
                .runDate(LocalDate.now())
                .status(RunStatus.RUNNING)
                .startedAt(LocalDateTime.now())
                .build();
        run = briefingRunRepository.save(run);

        try {
            List<EmailMessage> emails = emailFetcher.fetchEmailsFromLast24Hours();

            if (emails.isEmpty()) {
                log.info("No emails found.");
                run.setStatus(RunStatus.SUCCESS);
                run.setCompletedAt(LocalDateTime.now());
                briefingRunRepository.save(run);
                return;
            }

            EmailMessage latest = emails.get(0);

            if (processedEmailRepository.existsByEmailId(latest.getId())) {
                log.info("Email already processed: {}", latest.getSubject());
                telegramNotificationService.sendMessage("No new emails since last run.");
            } else {
                telegramNotificationService.sendMessage("New email: " + latest.getSubject());
                processedEmailRepository.save(ProcessedEmail.builder().emailId(latest.getId()).build());
                run.setEmailsFetched(1);
                log.info("New email processed: {}", latest.getSubject());
            }

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
