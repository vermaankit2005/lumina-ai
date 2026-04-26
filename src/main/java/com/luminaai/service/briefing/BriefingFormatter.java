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

//    public String format(AnalysisResult analysis, List<ActionTask> tasks, int emailsProcessed, LocalDate runDate) {
//        StringBuilder sb = new StringBuilder("\n\n");
//        sb.append("🌅 *Lumina AI Daily Briefing*\n");
//        sb.append("📅 ").append(runDate).append("\n");
//        sb.append("✉️ Processed ").append(emailsProcessed).append(" new email(s)\n\n");
//
//        sb.append("📧 *SUMMARY*\n");
//        sb.append(analysis.getSummary() != null ? analysis.getSummary() : "No summary available.").append("\n\n");
//
//        if (tasks.isEmpty()) {
//            sb.append("✅ No action items extracted.\n");
//        } else {
//            sb.append("✅ *ACTION ITEMS* (").append(tasks.size()).append(" new)\n");
//            for (int i = 0; i < tasks.size(); i++) {
//                ActionTask task = tasks.get(i);
//                sb.append("\n").append(i + 1).append(". 🟢 *").append(task.getTitle()).append("*\n");
//                if (task.getDescription() != null && !task.getDescription().isBlank()) {
//                    sb.append("   📌 ").append(task.getDescription()).append("\n");
//                }
//                if (task.getDeadlineDate() != null) {
//                    sb.append("   ⏰ Due: ").append(task.getDeadlineDate()).append("\n");
//                }
//            }
//        }
//
//        if (analysis.getProcessingNotes() != null) {
//            sb.append("\n_Note: ").append(analysis.getProcessingNotes()).append("_");
//        }
//        sb.append("\u200B");
//        return sb.toString();
//    }

    public String format(AnalysisResult analysis, List<ActionTask> tasks, int emailsProcessed, LocalDate runDate) {
        StringBuilder sb = new StringBuilder("\n\n");

        // HEADER
        sb.append("🗂 *LUMINA AI DAILY BRIEFING*\n");
        sb.append("📆 _Date: ").append(runDate).append("_\n");
        sb.append("✉️ _Emails Processed: ").append(emailsProcessed).append("_\n");
        sb.append("────────────────────────\n\n");

        // SUMMARY
        sb.append("*SUMMARY*\n");
        sb.append("• ").append(analysis.getSummary() != null ? analysis.getSummary() : "No summary available.").append("\n\n");

        // ACTION ITEMS
        if (tasks.isEmpty()) {
            sb.append("_No action items identified today._\n\n");
        } else {
            sb.append("*ACTION ITEMS* (").append(tasks.size()).append(")\n");
            for (int i = 0; i < tasks.size(); i++) {
                ActionTask task = tasks.get(i);
                // Use monospace for titles for emphasis
                sb.append("\n").append(i + 1).append(". `").append(task.getTitle()).append("`\n");
                if (task.getDescription() != null && !task.getDescription().isBlank()) {
                    sb.append("   • Description: ").append(task.getDescription()).append("\n");
                }
                if (task.getDeadlineDate() != null) {
                    sb.append("   • Due: ").append(task.getDeadlineDate()).append("\n");
                }
            }
            sb.append("\n");
        }

        // PROCESSING NOTES
        if (analysis.getProcessingNotes() != null && !analysis.getProcessingNotes().isBlank()) {
            sb.append("*Processing Notes:*\n");
            sb.append("_").append(analysis.getProcessingNotes()).append("_\n");
        }

        // FOOTER INVISIBLE CHAR
        sb.append("\u200B");
        return sb.toString();
    }
}
