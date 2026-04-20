package com.luminaai.service.briefing;

import com.luminaai.domain.enums.RunStatus;
import com.luminaai.domain.model.AnalysisResult;
import com.luminaai.domain.model.EmailMessage;
import com.luminaai.entity.ActionTask;
import com.luminaai.entity.BriefingRun;
import com.luminaai.entity.ProcessedEmail;
import com.luminaai.port.EmailAnalysisPort;
import com.luminaai.port.EmailFetcherPort;
import com.luminaai.port.NotificationPort;
import com.luminaai.repository.ActionTaskRepository;
import com.luminaai.repository.BriefingRunRepository;
import com.luminaai.repository.ProcessedEmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Primary implementation of {@link BriefingService}.
 *
 * <p>Execution steps:
 * <ol>
 *   <li>Fetch emails from the last 24 hours via {@link EmailFetcherPort}.</li>
 *   <li>Filter out already-processed messages using {@link ProcessedEmailRepository}.</li>
 *   <li>Analyse new messages with the {@link EmailAnalysisPort} (AI backend).</li>
 *   <li>Persist extracted {@link ActionTask} entities.</li>
 *   <li>Mark processed emails and deliver the briefing via {@link NotificationPort}.</li>
 *   <li>Record the outcome in {@link BriefingRun}.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyBriefingService implements BriefingService {

    private final EmailFetcherPort emailFetcher;
    private final EmailAnalysisPort emailAnalysis;
    private final NotificationPort notification;
    private final BriefingFormatter formatter;
    private final BriefingRunRepository briefingRunRepository;
    private final ActionTaskRepository actionTaskRepository;
    private final ProcessedEmailRepository processedEmailRepository;

    @Override
    public void runDailyBriefing() {
        log.info("Starting daily briefing run...");
        BriefingRun run = initRun();

        try {
            List<EmailMessage> allEmails = emailFetcher.fetchEmailsFromLast24Hours();

            if (allEmails.isEmpty()) {
                log.info("No emails found in the last 24 hours.");
                notification.send("📭 No new emails to process.");
                completeRun(run, 0, 0, null);
                return;
            }

            List<EmailMessage> newEmails = filterUnprocessed(allEmails);

            if (newEmails.isEmpty()) {
                log.info("All {} emails were already processed.", allEmails.size());
                notification.send("✅ No new emails since last run.");
                completeRun(run, allEmails.size(), 0, null);
                return;
            }

            log.info("Analysing {} new email(s) out of {} fetched.", newEmails.size(), allEmails.size());
            AnalysisResult analysis = emailAnalysis.analyze(newEmails);

            List<ActionTask> savedTasks = persistTasks(analysis, run);
            markAsProcessed(newEmails);

            String briefing = formatter.format(analysis, savedTasks, newEmails.size());
            notification.send(briefing);

            completeRun(run, newEmails.size(), savedTasks.size(), analysis.getSummary());
            log.info("Briefing complete — {} email(s) processed, {} task(s) extracted.",
                    newEmails.size(), savedTasks.size());

        } catch (Exception e) {
            log.error("Daily briefing run failed", e);
            failRun(run, e.getMessage());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private BriefingRun initRun() {
        BriefingRun run = BriefingRun.builder()
                .runDate(LocalDate.now())
                .status(RunStatus.RUNNING)
                .startedAt(LocalDateTime.now())
                .build();
        return briefingRunRepository.save(run);
    }

    private List<EmailMessage> filterUnprocessed(List<EmailMessage> emails) {
        return emails.stream()
                .filter(e -> !processedEmailRepository.existsByEmailId(e.getId()))
                .toList();
    }

    private List<ActionTask> persistTasks(AnalysisResult analysis, BriefingRun run) {
        if (analysis.getTasks() == null) return List.of();

        List<ActionTask> saved = new ArrayList<>();
        for (AnalysisResult.TaskItem item : analysis.getTasks()) {
            ActionTask task = ActionTask.builder()
                    .title(item.getTitle())
                    .description(item.getDescription())
                    .briefingRun(run)
                    .sourceEmailId(item.getSourceEmailId())
                    .sourceSender(item.getSourceSender())
                    .sourceSubject(item.getSourceSubject())
                    .deadlineDate(parseDate(item.getDeadlineDate()))
                    .build();
            saved.add(actionTaskRepository.save(task));
        }
        return saved;
    }

    private void markAsProcessed(List<EmailMessage> emails) {
        emails.forEach(e ->
                processedEmailRepository.save(ProcessedEmail.builder().emailId(e.getId()).build()));
    }

    private void completeRun(BriefingRun run, int emailsFetched, int tasksExtracted, String markdown) {
        run.setStatus(RunStatus.SUCCESS);
        run.setEmailsFetched(emailsFetched);
        run.setTasksExtracted(tasksExtracted);
        run.setBriefingMarkdown(markdown);
        run.setCompletedAt(LocalDateTime.now());
        briefingRunRepository.save(run);
    }

    private void failRun(BriefingRun run, String errorMessage) {
        run.setStatus(RunStatus.FAILED);
        run.setErrorMessage(errorMessage);
        run.setCompletedAt(LocalDateTime.now());
        briefingRunRepository.save(run);
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            log.warn("Could not parse deadline date: '{}'", dateStr);
            return null;
        }
    }
}
