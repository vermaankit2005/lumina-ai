package com.luminaai.repository;

import com.luminaai.entity.ActionTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActionTaskRepository extends JpaRepository<ActionTask, Long> {
}
