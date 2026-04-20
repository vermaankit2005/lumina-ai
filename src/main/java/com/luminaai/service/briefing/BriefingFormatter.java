package com.luminaai.service.briefing;

import com.luminaai.domain.model.AnalysisResult;
import com.luminaai.entity.ActionTask;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Converts an {@link AnalysisResult} and its persisted {@link ActionTask} list
 * into a human-readable Telegram Markdown briefing message.
 */
@Component
public class BriefingFormatter {

    public String format(AnalysisResult analysis, List<ActionTask> tasks, int emailsProcessed) {
        StringBuilder sb = new StringBuilder("\n\n");
        sb.append("🌅 *Lumina AI Daily Briefing*\n");
        sb.append("📅 ").append(LocalDate.now()).append("\n");
        sb.append("✉️ Processed ").append(emailsProcessed).append(" new email(s)\n\n");

        sb.append("📧 *SUMMARY*\n");
        sb.append(analysis.getSummary() != null ? analysis.getSummary() : "No summary available.").append("\n\n");

        if (tasks.isEmpty()) {
            sb.append("✅ No action items extracted.\n");
        } else {
            sb.append("✅ *ACTION ITEMS* (").append(tasks.size()).append(" new)\n");
            for (int i = 0; i < tasks.size(); i++) {
                ActionTask task = tasks.get(i);
                sb.append("\n").append(i + 1).append(". 🟢 *").append(task.getTitle()).append("*\n");
                if (task.getDescription() != null && !task.getDescription().isBlank()) {
                    sb.append("   📌 ").append(task.getDescription()).append("\n");
                }
                if (task.getDeadlineDate() != null) {
                    sb.append("   ⏰ Due: ").append(task.getDeadlineDate()).append("\n");
                }
            }
        }

        if (analysis.getProcessingNotes() != null) {
            sb.append("\n_Note: ").append(analysis.getProcessingNotes()).append("_");
        }
        sb.append("\n\n");
        return sb.toString();
    }
}
