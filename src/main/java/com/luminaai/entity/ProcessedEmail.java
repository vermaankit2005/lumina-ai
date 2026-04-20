package com.luminaai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_emails")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email_id", nullable = false, unique = true)
    private String emailId;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @PrePersist
    void prePersist() {
        processedAt = LocalDateTime.now();
    }
}
