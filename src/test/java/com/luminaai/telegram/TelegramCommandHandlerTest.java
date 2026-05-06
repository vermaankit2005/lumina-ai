package com.luminaai.telegram;

import com.luminaai.domain.enums.TaskPriority;
import com.luminaai.domain.enums.TaskStatus;
import com.luminaai.entity.ActionTask;
import com.luminaai.repository.ActionTaskRepository;
import com.luminaai.service.briefing.BriefingService;
import com.luminaai.service.task.TaskFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramCommandHandlerTest {

    @Mock private BriefingService briefingService;
    @Mock private ActionTaskRepository taskRepository;
    @Mock private TelegramSender telegramSender;
    @Mock private ConversationStateService conversationStateService;
    @Mock private DeadlineParser deadlineParser;

    private TelegramCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TelegramCommandHandler(
                briefingService, taskRepository, telegramSender,
                new TaskFormatter(), conversationStateService, deadlineParser);
    }

    // ── Existing commands ──────────────────────────────────────────────────────

    @Test
    void briefingCommandTriggersBriefingService() {
        handler.handle(ParsedCommand.of(Command.BRIEFING), "12345");
        verify(briefingService).runDailyBriefing();
        verifyNoInteractions(telegramSender);
    }

    @Test
    void tasksCommandSendsOpenTaskListWithKeyboard() {
        ActionTask task = buildTask(1L, "Reply to Alice", TaskPriority.HIGH);
        when(taskRepository.findByStatus(TaskStatus.OPEN)).thenReturn(List.of(task));

        handler.handle(ParsedCommand.of(Command.TASKS), "12345");

        verify(telegramSender).sendWithKeyboard(
                argThat(text -> text.contains("#1") && text.contains("Reply to Alice")),
                argThat(kb -> kb.getKeyboard().get(0).get(0).getCallbackData().equals("done:1"))
        );
    }

    @Test
    void tasksCommandSendsNoOpenTasksWhenEmpty() {
        when(taskRepository.findByStatus(TaskStatus.OPEN)).thenReturn(List.of());
        handler.handle(ParsedCommand.of(Command.TASKS), "12345");
        verify(telegramSender).send(argThat(m -> m.contains("No open tasks") && m.contains("/add")));
    }

    @Test
    void doneCommandMarksTaskAndSendsConfirmation() {
        ActionTask task = buildTask(2L, "Send report", TaskPriority.MEDIUM);
        when(taskRepository.findById(2L)).thenReturn(Optional.of(task));

        handler.handle(ParsedCommand.done(2), "12345");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
        verify(taskRepository).save(task);
        verify(telegramSender).send("✅ Task #2 marked done.");
    }

    @Test
    void doneCommandSendsNotFoundWhenTaskMissing() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());
        handler.handle(ParsedCommand.done(99), "12345");
        verify(telegramSender).send("Task #99 not found.");
    }

    @Test
    void doneCommandWithNoIdSendsUsageHint() {
        handler.handle(ParsedCommand.of(Command.DONE), "12345");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(telegramSender).send(captor.capture());
        assertThat(captor.getValue()).containsIgnoringCase("usage");
    }

    @Test
    void helpCommandSendsHelpMessage() {
        handler.handle(ParsedCommand.of(Command.HELP), "12345");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(telegramSender).send(captor.capture());
        assertThat(captor.getValue())
                .contains("/briefing").contains("/tasks").contains("/add").contains("/help");
    }

    @Test
    void unknownCommandSendsHelpMessageWithPrefix() {
        handler.handle(ParsedCommand.of(Command.UNKNOWN), "12345");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(telegramSender).send(captor.capture());
        assertThat(captor.getValue()).containsIgnoringCase("unknown").contains("/briefing");
    }

    // ── Callback ───────────────────────────────────────────────────────────────

    @Test
    void callbackDoneMarksTaskDone() {
        ActionTask task = buildTask(5L, "Pay bill", TaskPriority.HIGH);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));

        handler.handleCallback("done:5", "12345");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
        verify(taskRepository).save(task);
        verify(telegramSender).send("✅ Task #5 marked done.");
    }

    @Test
    void callbackWithUnknownDataIsIgnored() {
        handler.handleCallback("unknown:xyz", "12345");
        verifyNoInteractions(taskRepository, telegramSender);
    }

    // ── /add wizard ────────────────────────────────────────────────────────────

    @Test
    void addCommandStartsWizardAndAsksForTitle() {
        handler.handle(ParsedCommand.of(Command.ADD), "12345");

        verify(conversationStateService).start(12345L);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(telegramSender).send(captor.capture());
        assertThat(captor.getValue()).contains("Step 1/2").contains("/cancel");
    }

    @Test
    void wizardStep1CapturesTitleAndAdvancesToDeadlinePrompt() {
        ConversationState state = new ConversationState();
        when(conversationStateService.get(12345L)).thenReturn(Optional.of(state));

        handler.handleConversationInput("Call dentist", 12345L);

        assertThat(state.getTitle()).isEqualTo("Call dentist");
        assertThat(state.getStep()).isEqualTo(ConversationStep.AWAITING_DEADLINE);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(telegramSender).send(captor.capture());
        assertThat(captor.getValue()).contains("Step 2/2").contains("/skip");
    }

    @Test
    void wizardStep2WithParsedDateSavesTaskWithDeadline() {
        ConversationState state = new ConversationState();
        state.setTitle("Call dentist");
        state.setStep(ConversationStep.AWAITING_DEADLINE);
        when(conversationStateService.get(12345L)).thenReturn(Optional.of(state));
        when(deadlineParser.parse("15 May")).thenReturn(Optional.of(LocalDate.of(2026, 5, 15)));

        handler.handleConversationInput("15 May", 12345L);

        ArgumentCaptor<ActionTask> taskCaptor = ArgumentCaptor.forClass(ActionTask.class);
        verify(taskRepository).save(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getTitle()).isEqualTo("Call dentist");
        assertThat(taskCaptor.getValue().getDeadlineDate()).isEqualTo(LocalDate.of(2026, 5, 15));
        verify(conversationStateService).clear(12345L);
    }

    @Test
    void wizardStep2SkipSavesTaskWithoutDeadline() {
        ConversationState state = new ConversationState();
        state.setTitle("Read document");
        state.setStep(ConversationStep.AWAITING_DEADLINE);
        when(conversationStateService.get(12345L)).thenReturn(Optional.of(state));

        handler.handleConversationInput("/skip", 12345L);

        ArgumentCaptor<ActionTask> taskCaptor = ArgumentCaptor.forClass(ActionTask.class);
        verify(taskRepository).save(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getDeadlineDate()).isNull();
        assertThat(taskCaptor.getValue().getDeadlineRawText()).isNull();
        verify(conversationStateService).clear(12345L);
    }

    @Test
    void wizardStep2WithUnparsableDateStoresRawText() {
        ConversationState state = new ConversationState();
        state.setTitle("Submit form");
        state.setStep(ConversationStep.AWAITING_DEADLINE);
        when(conversationStateService.get(12345L)).thenReturn(Optional.of(state));
        when(deadlineParser.parse("end of month")).thenReturn(Optional.empty());

        handler.handleConversationInput("end of month", 12345L);

        ArgumentCaptor<ActionTask> taskCaptor = ArgumentCaptor.forClass(ActionTask.class);
        verify(taskRepository).save(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getDeadlineRawText()).isEqualTo("end of month");
        assertThat(taskCaptor.getValue().getDeadlineDate()).isNull();
    }

    @Test
    void cancelConversationClearsStateAndConfirms() {
        handler.cancelConversation(12345L);
        verify(conversationStateService).clear(12345L);
        verify(telegramSender).send(argThat(m -> m.contains("/add")));
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
