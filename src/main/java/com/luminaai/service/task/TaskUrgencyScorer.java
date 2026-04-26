package com.luminaai.service.task;

import com.luminaai.entity.ActionTask;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class TaskUrgencyScorer {

    public double score(ActionTask task) {
        if (task.getDeadlineDate() != null) {
            long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), task.getDeadlineDate());
            if (daysUntil < 0)  return 1000 + Math.abs(daysUntil) * 10.0;
            if (daysUntil == 0) return 900;
            if (daysUntil <= 2) return 800;
            if (daysUntil <= 7) return 500;
            return 200;
        }
        long age = ChronoUnit.DAYS.between(task.getCreatedAt().toLocalDate(), LocalDate.now());
        return Math.min(age * 5.0, 400);
    }
}
