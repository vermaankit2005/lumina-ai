package com.luminaai.service.task;

import com.luminaai.entity.ActionTask;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TaskFormatter {

    private static final String DIVIDER = "────────────────────────";

    public String formatList(List<ActionTask> tasks, String header) {
        StringBuilder sb = new StringBuilder(header).append("\n").append(DIVIDER).append("\n\n");
        for (ActionTask task : tasks) {
            sb.append("#").append(task.getId())
              .append(" ").append(priorityIcon(task))
              .append(" `").append(mdCode(task.getTitle())).append("`\n");
            if (task.getDeadlineDate() != null) {
                sb.append("   ⏰ Due: ").append(task.getDeadlineDate()).append("\n");
            } else if (task.getDeadlineRawText() != null && !task.getDeadlineRawText().isBlank()) {
                sb.append("   ⏰ Due: ").append(md(task.getDeadlineRawText())).append("\n");
            }
            sb.append("\n");
        }
        return sb.append("Reply `done #N` to close a task · /add to create a new one").toString();
    }

    public String formatAlert(ActionTask task) {
        StringBuilder sb = new StringBuilder("⚠️ *Deadline Alert*\n").append(DIVIDER).append("\n\n");
        sb.append("#").append(task.getId())
          .append(" ").append(priorityIcon(task))
          .append(" `").append(mdCode(task.getTitle())).append("`\n");
        if (task.getDeadlineDate() != null) {
            sb.append("📅 Due: ").append(task.getDeadlineDate()).append("\n");
        }
        return sb.append("\nReply `done #").append(task.getId()).append("` to dismiss.").toString();
    }

    private String priorityIcon(ActionTask task) {
        if (task.getPriority() == null) return "⚪";
        return switch (task.getPriority()) {
            case HIGH   -> "🔴";
            case MEDIUM -> "🟡";
            case LOW    -> "⚪";
        };
    }

    private String md(String text) {
        if (text == null) return "";
        return text.replace("_", " ").replace("*", " ").replace("[", "(").replace("]", ")");
    }

    private String mdCode(String text) {
        if (text == null) return "";
        return text.replace("`", "'");
    }
}
