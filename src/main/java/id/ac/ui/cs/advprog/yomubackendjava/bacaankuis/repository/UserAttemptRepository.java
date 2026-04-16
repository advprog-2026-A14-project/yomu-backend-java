package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.UserAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface UserAttemptRepository extends JpaRepository<UserAttempt, Long> {
    boolean existsByUserIdAndKuisId(UUID userId, String articleId);
}