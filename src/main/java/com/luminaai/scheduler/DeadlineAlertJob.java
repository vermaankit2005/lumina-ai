package com.luminaai.scheduler;

import com.luminaai.domain.enums.TaskStatus;
import com.luminaai.entity.ActionTask;
import com.luminaai.port.NotificationPort;
import com.luminaai.repository.ActionTaskRepository;
import com.luminaai.service.task.TaskFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeadlineAlertJob {

    private final ActionTaskRepository taskRepository;
    private final TaskFormatter taskFormatter;
    private final NotificationPort notificationPort;

    @Scheduled(cron = "0 0 8 * * *")
    public void sendDeadlineAlerts() {
        LocalDate today = LocalDate.now();
        List<ActionTask> candidates = taskRepository.findByStatusAndDeadlineDateBetween(
                TaskStatus.OPEN, today.minusDays(1), today.plusDays(2));

        List<ActionTask> toAlert = candidates.stream()
                .filter(t -> !today.equals(t.getReminderSentDate()))
                .toList();

        if (toAlert.isEmpty()) {
            log.info("No deadline alerts to send.");
            return;
        }
        log.info("Sending {} deadline alert(s).", toAlert.size());
        for (ActionTask task : toAlert) {
            notificationPort.send(taskFormatter.formatAlert(task));
            task.setReminderSentDate(today);
            taskRepository.save(task);
        }
    }
}
