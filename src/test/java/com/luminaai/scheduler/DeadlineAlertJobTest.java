package com.luminaai.scheduler;

import com.luminaai.domain.enums.TaskStatus;
import com.luminaai.entity.ActionTask;
import com.luminaai.port.NotificationPort;
import com.luminaai.repository.ActionTaskRepository;
import com.luminaai.service.task.TaskFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeadlineAlertJobTest {

    @Mock private ActionTaskRepository taskRepository;
    @Mock private NotificationPort notificationPort;

    private DeadlineAlertJob job;

    @BeforeEach
    void setUp() {
        job = new DeadlineAlertJob(taskRepository, new TaskFormatter(), notificationPort);
    }

    @Test
    void sendsAlertAndSetsReminderDateForEligibleTask() {
        ActionTask task = new ActionTask();
        task.setId(5L);
        task.setTitle("Submit tax documents");
        task.setDeadlineDate(LocalDate.now().plusDays(1));
        task.setReminderSentDate(LocalDate.now().minusDays(1));

        when(taskRepository.findByStatusAndDeadlineDateBetween(eq(TaskStatus.OPEN), any(), any()))
                .thenReturn(List.of(task));

        job.sendDeadlineAlerts();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(notificationPort).send(captor.capture());
        assertThat(captor.getValue()).contains("#5").contains("Submit tax documents").contains("done #5");
        assertThat(task.getReminderSentDate()).isEqualTo(LocalDate.now());
        verify(taskRepository).save(task);
    }

    @Test
    void skipsTaskAlreadyRemindedToday() {
        ActionTask task = new ActionTask();
        task.setId(3L);
        task.setTitle("Old reminder");
        task.setDeadlineDate(LocalDate.now());
        task.setReminderSentDate(LocalDate.now());

        when(taskRepository.findByStatusAndDeadlineDateBetween(eq(TaskStatus.OPEN), any(), any()))
                .thenReturn(List.of(task));

        job.sendDeadlineAlerts();

        verifyNoInteractions(notificationPort);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void skipsWhenNoCandidates() {
        when(taskRepository.findByStatusAndDeadlineDateBetween(eq(TaskStatus.OPEN), any(), any()))
                .thenReturn(List.of());

        job.sendDeadlineAlerts();

        verifyNoInteractions(notificationPort);
    }

    @Test
    void queriesCorrectDateRange() {
        LocalDate today = LocalDate.now();
        when(taskRepository.findByStatusAndDeadlineDateBetween(any(), any(), any()))
                .thenReturn(List.of());

        job.sendDeadlineAlerts();

        verify(taskRepository).findByStatusAndDeadlineDateBetween(
                TaskStatus.OPEN, today.minusDays(1), today.plusDays(2));
    }
}
