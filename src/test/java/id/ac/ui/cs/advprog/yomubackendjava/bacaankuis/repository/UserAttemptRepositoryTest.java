package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.UserAttempt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class UserAttemptRepositoryTest {

    @Autowired
    private UserAttemptRepository userAttemptRepository;

    @Test
    void save_whenSameUserAndQuizAlreadyExists_throwsDataIntegrityViolation() {
        UUID userId = UUID.randomUUID();
        String quizId = "article-001";

        UserAttempt first = new UserAttempt();
        first.setUserId(userId);
        first.setKuisId(quizId);
        first.setCompletedAt(LocalDateTime.now());

        UserAttempt duplicate = new UserAttempt();
        duplicate.setUserId(userId);
        duplicate.setKuisId(quizId);
        duplicate.setCompletedAt(LocalDateTime.now());

        userAttemptRepository.saveAndFlush(first);

        assertThrows(DataIntegrityViolationException.class, () -> {
            userAttemptRepository.saveAndFlush(duplicate);
        });
    }
}