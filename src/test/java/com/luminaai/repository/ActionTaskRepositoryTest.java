package com.luminaai.repository;

import com.luminaai.domain.enums.TaskPriority;
import com.luminaai.domain.enums.TaskStatus;
import com.luminaai.entity.ActionTask;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@TestPropertySource(properties = "spring.flyway.enabled=false")
class ActionTaskRepositoryTest {

    @Autowired
    private ActionTaskRepository repository;

    @Test
    void existsBySourceEmailIdReturnsTrueWhenExists() {
        String emailId = "test-email-id";
        ActionTask task = ActionTask.builder()
                .title("Test Task")
                .priority(TaskPriority.MEDIUM)
                .status(TaskStatus.OPEN)
                .sourceEmailId(emailId)
                .build();
        repository.save(task);

        assertTrue(repository.existsBySourceEmailId(emailId));
    }

    @Test
    void existsBySourceEmailIdReturnsFalseWhenNotExists() {
        assertFalse(repository.existsBySourceEmailId("non-existent"));
    }
}
