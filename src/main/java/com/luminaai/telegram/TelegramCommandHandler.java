package com.luminaai.telegram;

import com.luminaai.domain.enums.TaskStatus;
import com.luminaai.entity.ActionTask;
import com.luminaai.port.NotificationPort;
import com.luminaai.repository.ActionTaskRepository;
import com.luminaai.service.briefing.BriefingService;
import com.luminaai.service.task.TaskFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramCommandHandler {

    private final BriefingService briefingService;
    private final ActionTaskRepository taskRepository;
    private final NotificationPort notificationPort;
    private final TaskFormatter taskFormatter;

    public void handle(ParsedCommand command, String chatId) {
        switch (command.type()) {
            case BRIEFING -> runBriefing();
            case TASKS -> sendTaskList();
            case DONE -> markDone(command.taskId());
            case HELP -> notificationPort.send(helpMessage());
            case UNKNOWN -> notificationPort.send("Unknown command\\. " + helpMessage());
        }
    }

    private void runBriefing() {
        log.info("Manual briefing triggered via Telegram.");
        briefingService.runDailyBriefing();
    }

    private void sendTaskList() {
        List<ActionTask> open = taskRepository.findByStatus(TaskStatus.OPEN);
        if (open.isEmpty()) {
            notificationPort.send("No open tasks.");
            return;
        }
        String header = "📋 *Open Tasks* (" + open.size() + ")";
        notificationPort.send(taskFormatter.formatList(open, header));
    }

    private void markDone(Optional<Integer> taskId) {
        if (taskId.isEmpty()) {
            notificationPort.send("Usage: done #N  (e.g. done #2)");
            return;
        }

        long id = taskId.get().longValue();
        Optional<ActionTask> found = taskRepository.findById(id);
        if (found.isEmpty()) {
            notificationPort.send("Task #" + id + " not found.");
            return;
        }

        ActionTask task = found.get();
        task.setStatus(TaskStatus.DONE);
        task.setCompletedAt(LocalDateTime.now());
        taskRepository.save(task);
        log.info("Task #{} marked done.", id);
        notificationPort.send("✅ Task #" + id + " marked done.");
    }

    private String helpMessage() {
        return """
                *Lumina AI — Available Commands*

                /briefing — fetch and analyse your emails now
                /tasks — list all open action items
                `done #N` — mark task N as complete \\(e\\.g\\. `done #3`\\)
                /help — show this message""";
    }
}
