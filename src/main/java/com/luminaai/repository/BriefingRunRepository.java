package com.luminaai.repository;

import com.luminaai.domain.enums.RunStatus;
import com.luminaai.entity.BriefingRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface BriefingRunRepository extends JpaRepository<BriefingRun, Long> {
    Optional<BriefingRun> findByRunDateAndStatus(LocalDate runDate, RunStatus status);
    boolean existsByRunDate(LocalDate runDate);
}
