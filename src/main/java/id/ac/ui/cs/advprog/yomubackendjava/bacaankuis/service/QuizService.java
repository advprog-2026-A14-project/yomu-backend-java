package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizQuestionResponse;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizSyncRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.integration.QuizSyncClient;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.UserAttempt;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.QuizRepository;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.UserAttemptRepository;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.BadRequestException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class QuizService {
    private final UserAttemptRepository attemptRepository;
    private final QuizRepository quizRepository;
    private final QuizSyncClient quizSyncClient;

    public QuizService(
            UserAttemptRepository attemptRepository,
            QuizRepository quizRepository,
            QuizSyncClient quizSyncClient) {
        this.attemptRepository = attemptRepository;
        this.quizRepository = quizRepository;
        this.quizSyncClient = quizSyncClient;
    }

    public List<QuizQuestionResponse> getAvailableQuizzes(UUID userId, String articleId) {
        if (attemptRepository.existsByUserIdAndKuisId(userId, articleId)) {
            throw new ConflictException("Kuis sudah pernah diselesaikan");
        }

        return quizRepository.findByArticleId(articleId)
                .stream()
                .map(QuizQuestionResponse::new)
                .toList();
    }

    @Transactional
    public void submitAndSync(UUID authenticatedUserId, QuizSyncRequest request) {
        validateRequest(request);
        if (authenticatedUserId == null) {
            throw new BadRequestException("user_id wajib diisi");
        }
        request.setUserId(authenticatedUserId);

        if (attemptRepository.existsByUserIdAndKuisId(request.getUserId(), request.getArticleId())) {
            throw new ConflictException("Kuis sudah pernah dikerjakan!");
        }

        UserAttempt attempt = new UserAttempt();
        attempt.setUserId(request.getUserId());
        attempt.setKuisId(request.getArticleId());
        attempt.setCompletedAt(LocalDateTime.now());
        attemptRepository.save(attempt);

        quizSyncClient.sync(request);
    }

    private void validateRequest(QuizSyncRequest request) {
        if (request == null) {
            throw new BadRequestException("Request kuis tidak boleh kosong");
        }
        if (request.getArticleId() == null || request.getArticleId().isBlank()) {
            throw new BadRequestException("article_id wajib diisi");
        }
        if (request.getScore() < 0 || request.getScore() > 100) {
            throw new BadRequestException("score harus berada di antara 0 dan 100");
        }
        if (request.getAccuracy() < 0 || request.getAccuracy() > 100) {
            throw new BadRequestException("accuracy harus berada di antara 0 dan 100");
        }
    }
}