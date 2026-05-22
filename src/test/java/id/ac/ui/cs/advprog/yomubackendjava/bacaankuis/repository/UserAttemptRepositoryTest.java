package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.Article;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.UserAttempt;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.ArticleRepository;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.Role;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.UserEntity;
import id.ac.ui.cs.advprog.yomubackendjava.user.repo.UserRepository;
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

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void save_whenSameUserAndQuizAlreadyExists_throwsDataIntegrityViolation() {
        String quizId = "article-001";
        UUID userId = saveUser().getUserId();
        articleRepository.saveAndFlush(new Article(quizId, "Artikel", "Konten", "Kategori"));

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

    private UserEntity saveUser() {
        UserEntity user = new UserEntity();
        user.setUsername("attempt_user");
        user.setDisplayName("Attempt User");
        user.setRole(Role.PELAJAR);
        user.setPasswordHash("hash");
        return userRepository.saveAndFlush(user);
    }
}
