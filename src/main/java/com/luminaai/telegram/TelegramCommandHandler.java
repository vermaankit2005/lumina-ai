package com.luminaai.telegram;

import com.luminaai.domain.enums.TaskStatus;
import com.luminaai.entity.ActionTask;
import com.luminaai.repository.ActionTaskRepository;
import com.luminaai.service.briefing.BriefingService;
import com.luminaai.service.task.TaskFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramCommandHandler {

    private final BriefingService briefingService;
    private final ActionTaskRepository taskRepository;
    private final TelegramSender telegramSender;
    private final TaskFormatter taskFormatter;

    public void handle(ParsedCommand command, String chatId) {
        switch (command.type()) {
            case BRIEFING -> runBriefing();
            case TASKS    -> sendTaskList();
            case DONE     -> markDone(command.taskId());
            case HELP     -> telegramSender.send(helpMessage());
            case UNKNOWN  -> telegramSender.send("Unknown command\\. " + helpMessage());
        }
    }

    public void handleCallback(String data, String chatId) {
        if (data == null || !data.startsWith("done:")) {
            log.warn("Unrecognised callback data: {}", data);
            return;
        }
        try {
            long taskId = Long.parseLong(data.substring(5));
            markTaskDone(taskId);
        } catch (NumberFormatException e) {
            log.warn("Invalid task ID in callback data: {}", data);
        }
    }

    private void runBriefing() {
        log.info("Manual briefing triggered via Telegram.");
        briefingService.runDailyBriefing();
    }

    private void sendTaskList() {
        List<ActionTask> open = taskRepository.findByStatus(TaskStatus.OPEN);
        if (open.isEmpty()) {
            telegramSender.send("No open tasks.");
            return;
        }
        String header = "📋 *Open Tasks* (" + open.size() + ")";
        telegramSender.sendWithKeyboard(taskFormatter.formatList(open, header), buildDoneKeyboard(open));
    }

    private void markDone(Optional<Integer> taskId) {
        if (taskId.isEmpty()) {
            telegramSender.send("Usage: done #N  (e.g. done #2)");
            return;
        }
        markTaskDone(taskId.get().longValue());
    }

    private void markTaskDone(long id) {
        Optional<ActionTask> found = taskRepository.findById(id);
        if (found.isEmpty()) {
            telegramSender.send("Task #" + id + " not found.");
            return;
        }
        ActionTask task = found.get();
        task.setStatus(TaskStatus.DONE);
        task.setCompletedAt(LocalDateTime.now());
        taskRepository.save(task);
        log.info("Task #{} marked done.", id);
        telegramSender.send("✅ Task #" + id + " marked done.");
    }

    private InlineKeyboardMarkup buildDoneKeyboard(List<ActionTask> tasks) {
        List<List<InlineKeyboardButton>> rows = tasks.stream()
                .map(task -> {
                    InlineKeyboardButton btn = new InlineKeyboardButton("✅ Mark #" + task.getId() + " done");
                    btn.setCallbackData("done:" + task.getId());
                    return List.of(btn);
                })
                .collect(Collectors.toList());
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
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
