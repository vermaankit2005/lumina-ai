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

import java.time.LocalDate;
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
    private final ConversationStateService conversationStateService;
    private final DeadlineParser deadlineParser;

    public void handle(ParsedCommand command, String chatId) {
        switch (command.type()) {
            case BRIEFING -> runBriefing();
            case TASKS    -> sendTaskList();
            case DONE     -> markDone(command.taskId());
            case ADD      -> startAddWizard(Long.parseLong(chatId));
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

    public void handleConversationInput(String text, long chatId) {
        ConversationState state = conversationStateService.get(chatId).orElse(null);
        if (state == null) return;

        switch (state.getStep()) {
            case AWAITING_TITLE -> {
                if (text.isBlank()) {
                    telegramSender.send("Please type a task title.");
                    return;
                }
                state.setTitle(text.strip());
                state.setStep(ConversationStep.AWAITING_DEADLINE);
                telegramSender.send("""
                        📅 *New Task — Step 2/2*

                        When is it due? You can type:
                        • A date: `15 May`, `2026\\-05\\-15`, `tomorrow`
                        • Or /skip to add without a deadline""");
            }
            case AWAITING_DEADLINE -> {
                LocalDate deadline = null;
                String rawText = null;

                if (!text.equalsIgnoreCase("/skip")) {
                    Optional<LocalDate> parsed = deadlineParser.parse(text);
                    if (parsed.isPresent()) {
                        deadline = parsed.get();
                    } else {
                        rawText = text.strip();
                    }
                }

                ActionTask task = new ActionTask();
                task.setTitle(state.getTitle());
                task.setDeadlineDate(deadline);
                task.setDeadlineRawText(rawText);
                taskRepository.save(task);
                conversationStateService.clear(chatId);

                log.info("Task created via /add: '{}'", state.getTitle());

                StringBuilder confirm = new StringBuilder("✅ *Task added!*\n\n");
                confirm.append("`").append(state.getTitle().replace("`", "'")).append("`\n");
                if (deadline != null) {
                    confirm.append("⏰ Due: ").append(deadline);
                } else if (rawText != null) {
                    confirm.append("⏰ Due: ").append(rawText);
                } else {
                    confirm.append("_No deadline set_");
                }
                telegramSender.send(confirm.toString());
            }
        }
    }

    public void cancelConversation(long chatId) {
        conversationStateService.clear(chatId);
        telegramSender.send("Cancelled. Use /add to start again.");
    }

    private void startAddWizard(long chatId) {
        conversationStateService.start(chatId);
        telegramSender.send("""
                📝 *New Task — Step 1/2*

                What's the task? (e.g. "Call dentist")

                Type /cancel at any time to cancel.""");
    }

    private void runBriefing() {
        log.info("Manual briefing triggered via Telegram.");
        briefingService.runDailyBriefing();
    }

    private void sendTaskList() {
        List<ActionTask> open = taskRepository.findByStatus(TaskStatus.OPEN);
        if (open.isEmpty()) {
            telegramSender.send("No open tasks. Use /add to create one.");
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
                /add — add a new task with an optional deadline
                `done #N` — mark task N as complete \\(e\\.g\\. `done #3`\\)
                /help — show this message""";
    }
}
