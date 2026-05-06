package com.luminaai.scheduler;

import com.luminaai.domain.enums.TaskStatus;
import com.luminaai.entity.ActionTask;
import com.luminaai.port.NotificationPort;
import com.luminaai.repository.ActionTaskRepository;
import com.luminaai.service.task.TaskFormatter;
import com.luminaai.service.task.TaskUrgencyScorer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskDigestJobTest {

    @Mock private ActionTaskRepository taskRepository;
    @Mock private TaskUrgencyScorer urgencyScorer;
    @Mock private NotificationPort notificationPort;

    private TaskDigestJob job;

    @BeforeEach
    void setUp() {
        job = new TaskDigestJob(taskRepository, urgencyScorer, new TaskFormatter(), notificationPort);
    }

    @Test
    void skipsWhenNoOpenTasks() {
        when(taskRepository.findByStatus(TaskStatus.OPEN)).thenReturn(List.of());
        job.sendEveningDigest();
        verifyNoInteractions(notificationPort);
    }

    @Test
    void sendsDigestContainingTaskDetails() {
        ActionTask task = new ActionTask();
        task.setId(1L);
        task.setTitle("Write report");
        when(taskRepository.findByStatus(TaskStatus.OPEN)).thenReturn(List.of(task));

        job.sendEveningDigest();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(notificationPort).send(captor.capture());
        String msg = captor.getValue();
        assertThat(msg).contains("Evening Digest").contains("#1").contains("Write report");
    }

    @Test
    void deadlineShownWhenPresent() {
        LocalDate due = LocalDate.now().plusDays(1);
        ActionTask task = new ActionTask();
        task.setId(2L);
        task.setTitle("File taxes");
        task.setDeadlineDate(due);
        when(taskRepository.findByStatus(TaskStatus.OPEN)).thenReturn(List.of(task));

        job.sendEveningDigest();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(notificationPort).send(captor.capture());
        assertThat(captor.getValue()).contains(due.toString());
    }

    @Test
    void highPriorityTaskGetsRedEmoji() {
        ActionTask task = new ActionTask();
        task.setId(3L);
        task.setTitle("Overdue task");
        task.setPriority(com.luminaai.domain.enums.TaskPriority.HIGH);
        when(taskRepository.findByStatus(TaskStatus.OPEN)).thenReturn(List.of(task));

        job.sendEveningDigest();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(notificationPort).send(captor.capture());
        assertThat(captor.getValue()).contains("🔴");
    }
}
