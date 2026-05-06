package com.luminaai.scheduler;

import com.luminaai.domain.enums.TaskStatus;
import com.luminaai.entity.ActionTask;
import com.luminaai.port.NotificationPort;
import com.luminaai.repository.ActionTaskRepository;
import com.luminaai.service.task.TaskFormatter;
import com.luminaai.service.task.TaskUrgencyScorer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskDigestJob {

    private final ActionTaskRepository taskRepository;
    private final TaskUrgencyScorer urgencyScorer;
    private final TaskFormatter taskFormatter;
    private final NotificationPort notificationPort;

    @Scheduled(cron = "0 0 18 * * *")
    public void sendEveningDigest() {
        List<ActionTask> open = taskRepository.findByStatus(TaskStatus.OPEN);
        if (open.isEmpty()) {
            log.info("No open tasks — skipping evening digest.");
            return;
        }
        List<ActionTask> sorted = open.stream()
                .sorted(Comparator.comparingDouble(urgencyScorer::score).reversed())
                .toList();
        log.info("Sending evening task digest with {} task(s).", sorted.size());
        String header = "📋 *Evening Digest* — " + sorted.size() + " open task(s)";
        notificationPort.send(taskFormatter.formatList(sorted, header));
    }
}
