package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, String> {
    List<Quiz> findByArticleId(String articleId); // Required for GET /quizzes/{article_id}
}