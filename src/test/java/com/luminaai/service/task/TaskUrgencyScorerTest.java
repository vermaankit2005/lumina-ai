package com.luminaai.service.task;

import com.luminaai.entity.ActionTask;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TaskUrgencyScorerTest {

    private final TaskUrgencyScorer scorer = new TaskUrgencyScorer();

    @Test
    void overdueTaskScoresAbove1000() {
        assertThat(scorer.score(taskWithDeadline(LocalDate.now().minusDays(3)))).isGreaterThanOrEqualTo(1030);
    }

    @Test
    void taskDueTodayScores900() {
        assertThat(scorer.score(taskWithDeadline(LocalDate.now()))).isEqualTo(900);
    }

    @Test
    void taskDueTomorrowScores800() {
        assertThat(scorer.score(taskWithDeadline(LocalDate.now().plusDays(1)))).isEqualTo(800);
    }

    @Test
    void taskDueIn5DaysScores500() {
        assertThat(scorer.score(taskWithDeadline(LocalDate.now().plusDays(5)))).isEqualTo(500);
    }

    @Test
    void taskDueIn10DaysScores200() {
        assertThat(scorer.score(taskWithDeadline(LocalDate.now().plusDays(10)))).isEqualTo(200);
    }

    @Test
    void undatedTaskScoresByAge() {
        ActionTask task = new ActionTask();
        task.setCreatedAt(LocalDateTime.now().minusDays(10));
        assertThat(scorer.score(task)).isEqualTo(50.0);
    }

    @Test
    void undatedTaskScoreCapsAt400() {
        ActionTask task = new ActionTask();
        task.setCreatedAt(LocalDateTime.now().minusDays(200));
        assertThat(scorer.score(task)).isEqualTo(400.0);
    }

    private ActionTask taskWithDeadline(LocalDate deadline) {
        ActionTask task = new ActionTask();
        task.setDeadlineDate(deadline);
        task.setCreatedAt(LocalDateTime.now());
        return task;
    }
}
