package com.luminaai.repository;

import com.luminaai.entity.ProcessedEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEmailRepository extends JpaRepository<ProcessedEmail, Long> {
    boolean existsByEmailId(String emailId);
}
