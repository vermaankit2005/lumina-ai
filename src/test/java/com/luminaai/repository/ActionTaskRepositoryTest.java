package com.luminaai.repository;

import com.luminaai.entity.ActionTask;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DEFAULT_NULL_ORDERING=HIGH"
})
class ActionTaskRepositoryTest {

    @Autowired
    private ActionTaskRepository repository;

    @Test
    void existsBySourceEmailIdReturnsTrueWhenExists() {
        String emailId = "test-email-id";
        ActionTask task = ActionTask.builder()
                .title("Test Task")
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
