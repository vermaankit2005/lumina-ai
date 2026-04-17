package com.luminaai.entity;

import com.luminaai.domain.enums.RunStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "briefing_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BriefingRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_date", nullable = false)
    private LocalDate runDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RunStatus status;

    @Column(name = "emails_fetched")
    private Integer emailsFetched;

    @Column(name = "tasks_extracted")
    private Integer tasksExtracted;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "briefing_markdown")
    private String briefingMarkdown;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = RunStatus.PENDING;
        }
    }
}
