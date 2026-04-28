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

    public String format(AnalysisResult analysis, List<ActionTask> tasks, int emailsProcessed, LocalDate runDate) {
        StringBuilder sb = new StringBuilder("\n\n");

        // HEADER
        sb.append("🗂 *LUMINA AI DAILY BRIEFING*\n");
        sb.append("📆 _").append(runDate).append("  •  ").append(emailsProcessed).append(" emails_\n");
        sb.append("────────────────────────\n\n");

        // SUMMARY — single mood line, no bullet prefix
        sb.append(analysis.getSummary() != null ? analysis.getSummary() : "Inbox processed.").append("\n\n");

        // NOTABLE INBOX — awareness items that produced no action item
        List<String> highlights = analysis.getInboxHighlights();
        if (highlights != null && !highlights.isEmpty()) {
            sb.append("*Notable inbox*\n");
            for (String item : highlights) {
                sb.append("  ").append(item).append("\n");
            }
            sb.append("\n");
        }

        // ACTION ITEMS
        if (tasks.isEmpty()) {
            sb.append("_Nothing needs your attention today._\n");
        } else {
            sb.append("*Action items* (").append(tasks.size()).append(")\n");
            for (int i = 0; i < tasks.size(); i++) {
                ActionTask task = tasks.get(i);
                String icon = task.getPriority() != null ? priorityIcon(task.getPriority().name()) : "";
                sb.append("\n").append(i + 1).append(". ").append(icon)
                  .append(" `").append(task.getTitle()).append("`\n");
                if (task.getDescription() != null && !task.getDescription().isBlank()) {
                    sb.append("   ").append(task.getDescription()).append("\n");
                }
                if (task.getDeadlineDate() != null) {
                    sb.append("   ⏰ Due: ").append(task.getDeadlineDate()).append("\n");
                }
            }
            sb.append("\n");
        }

        // PROCESSING NOTES
        if (analysis.getProcessingNotes() != null && !analysis.getProcessingNotes().isBlank()) {
            sb.append("_").append(analysis.getProcessingNotes()).append("_\n");
        }

        sb.append("​");
        return sb.toString();
    }

    private String priorityIcon(String priority) {
        return switch (priority) {
            case "HIGH"   -> "🔴";
            case "MEDIUM" -> "🟡";
            case "LOW"    -> "🟢";
            default       -> "";
        };
    }
}
