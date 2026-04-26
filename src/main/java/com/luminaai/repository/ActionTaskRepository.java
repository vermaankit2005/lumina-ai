package com.luminaai.repository;

import com.luminaai.domain.enums.TaskStatus;
import com.luminaai.entity.ActionTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ActionTaskRepository extends JpaRepository<ActionTask, Long> {
    List<ActionTask> findByStatus(TaskStatus status);
    List<ActionTask> findByStatusAndDeadlineDateBetween(TaskStatus status, LocalDate from, LocalDate to);
}
