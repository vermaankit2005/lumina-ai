package com.luminaai.entity;

import com.luminaai.domain.enums.TaskPriority;
import com.luminaai.domain.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "action_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private UUID externalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "briefing_run_id")
    private BriefingRun briefingRun;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Column(name = "source_email_id")
    private String sourceEmailId;

    @Column(name = "source_sender")
    private String sourceSender;

    @Column(name = "source_subject", length = 500)
    private String sourceSubject;

    @Column(name = "deadline_date")
    private LocalDate deadlineDate;

    @Column(name = "deadline_raw_text", length = 200)
    private String deadlineRawText;

    @Column(name = "confidence")
    private Float confidence;

    @Column(name = "reminder_sent_date")
    private LocalDate reminderSentDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (externalId == null) {
            externalId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (priority == null) {
            priority = TaskPriority.MEDIUM;
        }
        if (status == null) {
            status = TaskStatus.OPEN;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
