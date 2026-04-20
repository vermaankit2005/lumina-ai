package com.luminaai.repository;

import com.luminaai.entity.ActionTask;
import com.luminaai.domain.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActionTaskRepository extends JpaRepository<ActionTask, Long> {
    Optional<ActionTask> findByExternalId(UUID externalId);
    List<ActionTask> findByStatus(TaskStatus status);
}
