package com.luminaai.runner;

import com.luminaai.domain.enums.RunStatus;
import com.luminaai.domain.model.EmailMessage;
import com.luminaai.domain.model.LLMAnalysisResult;
import com.luminaai.entity.ActionTask;
import com.luminaai.entity.BriefingRun;
import com.luminaai.repository.ActionTaskRepository;
import com.luminaai.repository.BriefingRunRepository;
import com.luminaai.service.ai.LLMService;
import com.luminaai.service.gmail.GmailFetchService;
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

    private final GmailFetchService gmailFetchService;
    private final LLMService llmService;
    private final TelegramNotificationService telegramNotificationService;
    private final BriefingRunRepository briefingRunRepository;
    private final ActionTaskRepository actionTaskRepository;

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
            // 1. Fetch latest email
            EmailMessage email = gmailFetchService.fetchLatestEmailMessage();

            if (email == null) {
                log.info("No emails found.");
                telegramNotificationService.sendMessage("📭 No new emails to process.");
                complete(run, 0, 0, null);
                return;
            }

            // 2. Deduplication check
            if (actionTaskRepository.existsBySourceEmailId(email.getId())) {
                log.info("Email already processed: {}", email.getSubject());
                telegramNotificationService.sendMessage("✅ No new emails since last run.");
                complete(run, 0, 0, null);
                return;
            }

            // 3. Send to LLM for analysis
            LLMAnalysisResult analysis = llmService.analyzeEmails(List.of(email));

            // 4. Persist extracted tasks
            List<ActionTask> savedTasks = new ArrayList<>();
            if (analysis.getTasks() != null) {
                for (LLMAnalysisResult.TaskItem item : analysis.getTasks()) {
                    if (actionTaskRepository.existsBySourceEmailId(item.getSourceEmailId())) {
                        continue;
                    }
                    ActionTask task = ActionTask.builder()
                            .title(item.getTitle())
                            .description(item.getDescription())
                            .briefingRun(run)
                            .sourceEmailId(item.getSourceEmailId() != null ? item.getSourceEmailId() : email.getId())
                            .sourceSender(item.getSourceSender())
                            .sourceSubject(item.getSourceSubject())
                            .deadlineDate(parseDate(item.getDeadlineDate()))
                            .build();
                    savedTasks.add(actionTaskRepository.save(task));
                }
            }

            // If LLM extracted no tasks but email is new, record it anyway for dedup
            if (savedTasks.isEmpty()) {
                ActionTask placeholder = ActionTask.builder()
                        .title("Processed: " + email.getSubject())
                        .briefingRun(run)
                        .sourceEmailId(email.getId())
                        .sourceSender(email.getFrom())
                        .sourceSubject(email.getSubject())
                        .build();
                actionTaskRepository.save(placeholder);
            }

            // 5. Format and send Telegram briefing
            String briefing = formatBriefing(analysis, savedTasks);
            telegramNotificationService.sendMessage(briefing);

            complete(run, 1, savedTasks.size(), analysis.getSummary());
            log.info("Sprint 4 run complete. Tasks extracted: {}", savedTasks.size());

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

    private String formatBriefing(LLMAnalysisResult analysis, List<ActionTask> tasks) {
        StringBuilder sb = new StringBuilder();
        sb.append("🌅 *Lumina AI Briefing*\n");
        sb.append("📅 ").append(LocalDate.now()).append("\n\n");
        sb.append("📧 *EMAIL SUMMARY*\n");
        sb.append(analysis.getSummary() != null ? analysis.getSummary() : "No summary available.").append("\n\n");

        if (!tasks.isEmpty()) {
            sb.append("✅ *ACTION ITEMS* (").append(tasks.size()).append(" new)\n");
            for (ActionTask task : tasks) {
                String emoji = "🟢";
                sb.append(emoji).append(" ").append(task.getTitle()).append("\n");
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
