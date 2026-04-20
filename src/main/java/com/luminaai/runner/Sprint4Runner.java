package com.luminaai.runner;

import com.luminaai.domain.enums.RunStatus;
import com.luminaai.domain.model.EmailMessage;
import com.luminaai.domain.model.LLMAnalysisResult;
import com.luminaai.entity.ActionTask;
import com.luminaai.entity.BriefingRun;
import com.luminaai.entity.ProcessedEmail;
import com.luminaai.repository.ActionTaskRepository;
import com.luminaai.repository.BriefingRunRepository;
import com.luminaai.repository.ProcessedEmailRepository;
import com.luminaai.service.ai.LLMService;
import com.luminaai.service.gmail.EmailFetcher;
import com.luminaai.service.notification.TelegramNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class Sprint4Runner implements CommandLineRunner {

    private final EmailFetcher emailFetcher;
    private final LLMService llmService;
    private final TelegramNotificationService telegramNotificationService;
    private final BriefingRunRepository briefingRunRepository;
    private final ActionTaskRepository actionTaskRepository;
    private final ProcessedEmailRepository processedEmailRepository;

    @Override
    public void run(String... args) {
        log.info("Sprint 4: starting AI-powered briefing run...");

        BriefingRun run = BriefingRun.builder()
                .runDate(LocalDate.now())
                .status(RunStatus.RUNNING)
                .startedAt(LocalDateTime.now())
                .build();
        run = briefingRunRepository.save(run);

        try {
            List<EmailMessage> emails = emailFetcher.fetchEmailsFromLast24Hours();

            if (emails.isEmpty()) {
                log.info("No emails found from last 24 hours.");
                telegramNotificationService.sendMessage("📭 No new emails to process.");
                complete(run, 0, 0, null);
                return;
            }

            List<EmailMessage> newEmails = emails.stream()
                    .filter(email -> !processedEmailRepository.existsByEmailId(email.getId()))
                    .toList();

            if (newEmails.isEmpty()) {
                log.info("All {} emails already processed.", emails.size());
                telegramNotificationService.sendMessage("✅ No new emails since last run.");
                complete(run, emails.size(), 0, null);
                return;
            }

            log.info("Processing {} new emails out of {} total.", newEmails.size(), emails.size());

            LLMAnalysisResult analysis = llmService.analyzeEmails(newEmails);

            List<ActionTask> savedTasks = new ArrayList<>();
            if (analysis.getTasks() != null) {

                for (LLMAnalysisResult.TaskItem item : analysis.getTasks()) {

                    ActionTask task = ActionTask.builder()
                            .title(item.getTitle())
                            .description(item.getDescription())
                            .briefingRun(run)
                            .sourceEmailId(item.getSourceEmailId())
                            .sourceSender(item.getSourceSender())
                            .sourceSubject(item.getSourceSubject())
                            .deadlineDate(parseDate(item.getDeadlineDate()))
                            .build();
                    savedTasks.add(actionTaskRepository.save(task));
                }
            }

            for (EmailMessage email : newEmails) {
                processedEmailRepository.save(ProcessedEmail.builder()
                        .emailId(email.getId())
                        .build());
            }

            String briefing = formatBriefing(analysis, savedTasks, newEmails.size());
            telegramNotificationService.sendMessage(briefing);

            complete(run, newEmails.size(), savedTasks.size(), analysis.getSummary());
            log.info("Sprint 4 run complete. Processed {} emails, extracted {} tasks.", newEmails.size(), savedTasks.size());

        } catch (Exception e) {
            log.error("Sprint 4 run failed", e);
            run.setStatus(RunStatus.FAILED);
            run.setErrorMessage(e.getMessage());
            run.setCompletedAt(LocalDateTime.now());
            briefingRunRepository.save(run);
        }
    }

    private void complete(BriefingRun run, int emailsFetched, int tasksExtracted, String markdown) {
        run.setStatus(RunStatus.SUCCESS);
        run.setEmailsFetched(emailsFetched);
        run.setTasksExtracted(tasksExtracted);
        run.setBriefingMarkdown(markdown);
        run.setCompletedAt(LocalDateTime.now());
        briefingRunRepository.save(run);
    }

    private String formatBriefing(LLMAnalysisResult analysis, List<ActionTask> tasks, int emailsProcessed) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n");
        sb.append("🌅 *Lumina AI Briefing*\n");
        sb.append("📅 ").append(LocalDate.now()).append("\n");
        sb.append("✉️ Processed ").append(emailsProcessed).append(" email(s) from last 24 hours\n\n");
        sb.append("📧 *EMAIL SUMMARY*\n");
        sb.append(analysis.getSummary() != null ? analysis.getSummary() : "No summary available.").append("\n\n");

        if (!tasks.isEmpty()) {
            sb.append("✅ *ACTION ITEMS* (").append(tasks.size()).append(" new)\n");
            for (int i = 0; i < tasks.size(); i++) {
                ActionTask task = tasks.get(i);
                sb.append("\n").append(i + 1).append(". 🟢 ").append(task.getTitle()).append("\n");
                if (task.getDescription() != null) {
                    sb.append("   📌 ").append(task.getDescription()).append("\n");
                }
            }
        } else {
            sb.append("✅ No action items extracted.\n");
        }
        if (analysis.getProcessingNotes() != null) {
            sb.append("\n_Note: ").append(analysis.getProcessingNotes()).append("_");
        }

        sb.append("\n\n");
        return sb.toString();
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            log.warn("Could not parse deadline date: {}", dateStr);
            return null;
        }
    }
}
