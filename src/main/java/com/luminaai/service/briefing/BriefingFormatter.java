package com.luminaai.service.briefing;

import com.luminaai.domain.model.AnalysisResult;
import com.luminaai.entity.ActionTask;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Converts an {@link AnalysisResult} and its persisted {@link ActionTask} list
 * into a Telegram Markdown v1 briefing message.
 *
 * All LLM-generated strings are passed through {@link #md(String)} before
 * insertion to prevent unbalanced Markdown tokens from breaking the parse.
 */
@Component
public class BriefingFormatter {

    public String format(AnalysisResult analysis, List<ActionTask> tasks, int emailsProcessed, LocalDate runDate) {
        StringBuilder sb = new StringBuilder("\n\n");

        // HEADER
        int important = analysis.getImportantThreadCount();
        sb.append("🗂 *LUMINA AI DAILY BRIEFING*\n");
        sb.append("📆 _").append(runDate).append("_");
        if (emailsProcessed > 0) {
            sb.append("  _•  ").append(emailsProcessed).append(" emails");
            if (important > 0) sb.append(", ").append(important).append(" notable");
            sb.append("_");
        }
        sb.append("\n────────────────────────\n\n");

        // SUMMARY — single mood line from LLM, sanitized
        sb.append(md(analysis.getSummary() != null ? analysis.getSummary() : "Inbox processed."))
          .append("\n\n");

        // NOTABLE INBOX — awareness items that produced no action item (deduped in LLMService)
        List<AnalysisResult.InboxHighlight> highlights = analysis.getInboxHighlights();
        if (highlights != null && !highlights.isEmpty()) {
            sb.append("*Notable inbox*\n");
            for (AnalysisResult.InboxHighlight h : highlights) {
                if (h.getText() != null && !h.getText().isBlank()) {
                    // Highlights start with emoji by prompt convention — only sanitize the text portion
                    sb.append("  ").append(mdHighlight(h.getText())).append("\n");
                }
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
                  .append(" `").append(mdCode(task.getTitle())).append("`\n");
                if (task.getDescription() != null && !task.getDescription().isBlank()) {
                    sb.append("   ").append(md(task.getDescription())).append("\n");
                }
                // Prefer parsed date; fall back to raw text from LLM when date wasn't parseable
                if (task.getDeadlineDate() != null) {
                    sb.append("   ⏰ Due: ").append(task.getDeadlineDate()).append("\n");
                } else if (task.getDeadlineRawText() != null && !task.getDeadlineRawText().isBlank()) {
                    sb.append("   ⏰ Due: ").append(md(task.getDeadlineRawText())).append("\n");
                }
            }
            sb.append("\n");
        }

        // PROCESSING NOTES — only shown when analysis had a problem
        if (analysis.getProcessingNotes() != null && !analysis.getProcessingNotes().isBlank()) {
            sb.append("_").append(md(analysis.getProcessingNotes())).append("_\n");
        }

        sb.append("​");
        return sb.toString();
    }

    // ── Markdown helpers ──────────────────────────────────────────────────────

    /**
     * Sanitizes a string for safe insertion as plain text in Telegram Markdown v1.
     * Replaces characters that would create unbalanced formatting tokens.
     */
    private String md(String text) {
        if (text == null) return "";
        return text
                .replace("_", " ")   // italic marker
                .replace("*", " ")   // bold marker
                .replace("[", "(")   // link open
                .replace("]", ")");  // link close
    }

    /** Sanitizes a string for insertion inside a `code span` — strips backticks only. */
    private String mdCode(String text) {
        if (text == null) return "";
        return text.replace("`", "'");
    }

    /**
     * For highlights that intentionally start with emoji, preserves the leading
     * emoji and sanitizes the rest of the string.
     */
    private String mdHighlight(String text) {
        if (text == null) return "";
        // Emoji are multi-byte; find the first ASCII char and sanitize from there
        int firstAscii = 0;
        while (firstAscii < text.length() && text.charAt(firstAscii) > 127) firstAscii++;
        String emoji = text.substring(0, firstAscii);
        String rest  = md(text.substring(firstAscii));
        return emoji + rest;
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
