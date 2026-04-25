package com.luminaai.telegram;

import com.luminaai.domain.enums.TaskPriority;
import com.luminaai.domain.enums.TaskStatus;
import com.luminaai.entity.ActionTask;
import com.luminaai.port.NotificationPort;
import com.luminaai.repository.ActionTaskRepository;
import com.luminaai.service.briefing.BriefingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramCommandHandlerTest {

    @Mock private BriefingService briefingService;
    @Mock private ActionTaskRepository taskRepository;
    @Mock private NotificationPort notificationPort;

    private TelegramCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TelegramCommandHandler(briefingService, taskRepository, notificationPort);
    }

    @Test
    void briefingCommandTriggersBriefingService() {
        handler.handle(ParsedCommand.of(Command.BRIEFING), "12345");
        verify(briefingService).runDailyBriefing();
        verifyNoInteractions(notificationPort);
    }

    @Test
    void tasksCommandSendsOpenTaskList() {
        ActionTask task = buildTask(1L, "Reply to Alice", TaskPriority.HIGH);
        when(taskRepository.findByStatus(TaskStatus.OPEN)).thenReturn(List.of(task));

        handler.handle(ParsedCommand.of(Command.TASKS), "12345");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(notificationPort).send(captor.capture());
        assertThat(captor.getValue()).contains("#1").contains("🔴").contains("Reply to Alice");
    }

    @Test
    void tasksCommandSendsNoOpenTasksWhenEmpty() {
        when(taskRepository.findByStatus(TaskStatus.OPEN)).thenReturn(List.of());

        handler.handle(ParsedCommand.of(Command.TASKS), "12345");

        verify(notificationPort).send("No open tasks.");
    }

    @Test
    void doneCommandMarksTaskAndSendsConfirmation() {
        ActionTask task = buildTask(2L, "Send report", TaskPriority.MEDIUM);
        when(taskRepository.findById(2L)).thenReturn(Optional.of(task));

        handler.handle(ParsedCommand.done(2), "12345");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(task.getCompletedAt()).isNotNull();
        verify(taskRepository).save(task);
        verify(notificationPort).send("✅ Task #2 marked done.");
    }

    @Test
    void doneCommandSendsNotFoundWhenTaskMissing() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        handler.handle(ParsedCommand.done(99), "12345");

        verify(notificationPort).send("Task #99 not found.");
        verify(taskRepository, never()).save(any());
    }

    @Test
    void doneCommandWithNoIdSendsUsageHint() {
        handler.handle(ParsedCommand.of(Command.DONE), "12345");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(notificationPort).send(captor.capture());
        assertThat(captor.getValue()).containsIgnoringCase("usage");
        verifyNoInteractions(taskRepository);
    }

    @Test
    void unknownCommandSendsHelpMessage() {
        handler.handle(ParsedCommand.of(Command.UNKNOWN), "12345");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(notificationPort).send(captor.capture());
        assertThat(captor.getValue()).contains("/briefing").contains("/tasks").contains("done #N");
    }

    private ActionTask buildTask(Long id, String title, TaskPriority priority) {
        ActionTask task = new ActionTask();
        task.setId(id);
        task.setTitle(title);
        task.setPriority(priority);
        task.setStatus(TaskStatus.OPEN);
        return task;
    }
}
