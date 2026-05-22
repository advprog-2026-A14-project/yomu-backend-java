package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizQuestionResponse;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizSyncRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.integration.QuizSyncClient;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.UserAttempt;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.ArticleRepository;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.QuizRepository;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.UserAttemptRepository;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.BadRequestException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.ConflictException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.NotFoundException;
import id.ac.ui.cs.advprog.yomubackendjava.common.security.SecuritySanitizer;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.OutboxService;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizAnswerRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizSubmitRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.Quiz;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizSubmitResult;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class QuizService {
    private static final String QUIZ_ALREADY_ATTEMPTED_MESSAGE = "Kuis sudah pernah dikerjakan!";

    private final UserAttemptRepository attemptRepository;
    private final ArticleRepository articleRepository;
    private final QuizRepository quizRepository;
    private final QuizSyncClient quizSyncClient;
    private final OutboxService outboxService;

    public QuizService(
            UserAttemptRepository attemptRepository,
            ArticleRepository articleRepository,
            QuizRepository quizRepository,
            QuizSyncClient quizSyncClient,
            OutboxService outboxService) {
        this.attemptRepository = attemptRepository;
        this.articleRepository = articleRepository;
        this.quizRepository = quizRepository;
        this.quizSyncClient = quizSyncClient;
        this.outboxService = outboxService;
    }

    public List<QuizQuestionResponse> getAvailableQuizzes(UUID userId, String articleId) {
        validateArticleId(articleId);
        ensureArticleExists(articleId);
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
        ensureArticleExists(request.getArticleId());

        if (attemptRepository.existsByUserIdAndKuisId(request.getUserId(), request.getArticleId())) {
            throw new ConflictException(QUIZ_ALREADY_ATTEMPTED_MESSAGE);
        }

        UserAttempt attempt = new UserAttempt();
        attempt.setUserId(request.getUserId());
        attempt.setKuisId(request.getArticleId());
        attempt.setCompletedAt(LocalDateTime.now());

        try {
            attemptRepository.saveAndFlush(attempt);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(QUIZ_ALREADY_ATTEMPTED_MESSAGE);
        }

        syncQuizCompletion(request);
    }

    private void validateRequest(QuizSyncRequest request) {
        if (request == null) {
            throw new BadRequestException("Request kuis tidak boleh kosong");
        }
        validateArticleId(request.getArticleId());
        if (request.getScore() < 0 || request.getScore() > 100) {
            throw new BadRequestException("score harus berada di antara 0 dan 100");
        }
        if (request.getAccuracy() < 0 || request.getAccuracy() > 100) {
            throw new BadRequestException("accuracy harus berada di antara 0 dan 100");
        }
    }

    @Transactional
    public QuizSubmitResult submitAndSync(UUID authenticatedUserId, String articleId, QuizSubmitRequest request) {
        validateSubmitRequest(authenticatedUserId, articleId, request);
        ensureArticleExists(articleId);

        if (attemptRepository.existsByUserIdAndKuisId(authenticatedUserId, articleId)) {
            throw new ConflictException(QUIZ_ALREADY_ATTEMPTED_MESSAGE);
        }

        List<Quiz> quizzes = quizRepository.findByArticleId(articleId);
        if (quizzes.isEmpty()) {
            throw new BadRequestException("Kuis tidak tersedia untuk artikel ini");
        }

        Map<String, String> submittedAnswers = mapSubmittedAnswers(request.getAnswers());
        validateAnswersBelongToArticle(submittedAnswers.keySet(), quizzes);

        long correctCount = quizzes.stream()
                .filter(quiz -> isCorrectAnswer(quiz, submittedAnswers.get(quiz.getId())))
                .count();

        double percentage = ((double) correctCount / quizzes.size()) * 100.0;

        UserAttempt attempt = new UserAttempt();
        attempt.setUserId(authenticatedUserId);
        attempt.setKuisId(articleId);
        attempt.setCompletedAt(LocalDateTime.now());

        try {
            attemptRepository.saveAndFlush(attempt);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(QUIZ_ALREADY_ATTEMPTED_MESSAGE);
        }

        QuizSyncRequest syncRequest = new QuizSyncRequest(
                authenticatedUserId,
                articleId,
                percentage,
                percentage
        );

        syncQuizCompletion(syncRequest);

        return new QuizSubmitResult(
                percentage,
                percentage,
                correctCount,
                quizzes.size()
        );
    }

    private void validateSubmitRequest(UUID userId, String articleId, QuizSubmitRequest request) {
        if (userId == null) {
            throw new BadRequestException("user_id wajib diisi");
        }
        validateArticleId(articleId);
        if (request == null || request.getAnswers() == null || request.getAnswers().isEmpty()) {
            throw new BadRequestException("answers wajib diisi");
        }
    }

    private Map<String, String> mapSubmittedAnswers(List<QuizAnswerRequest> answers) {
        Map<String, String> mappedAnswers = new HashMap<>();

        for (QuizAnswerRequest answer : answers) {
            if (answer.getQuizId() == null || answer.getQuizId().isBlank()) {
                throw new BadRequestException("quiz_id wajib diisi");
            }
            if (answer.getAnswer() == null || answer.getAnswer().isBlank()) {
                throw new BadRequestException("answer wajib diisi");
            }
            if (mappedAnswers.containsKey(answer.getQuizId())) {
                throw new BadRequestException("quiz_id tidak boleh duplikat");
            }

            mappedAnswers.put(answer.getQuizId(), answer.getAnswer());
        }

        return mappedAnswers;
    }

    private void validateAnswersBelongToArticle(Set<String> submittedQuizIds, List<Quiz> quizzes) {
        Set<String> expectedQuizIds = new HashSet<>();
        for (Quiz quiz : quizzes) {
            expectedQuizIds.add(quiz.getId());
        }

        if (!expectedQuizIds.equals(submittedQuizIds)) {
            throw new BadRequestException("answers harus mencakup seluruh quiz_id dari artikel ini");
        }
    }

    private boolean isCorrectAnswer(Quiz quiz, String submittedAnswer) {
        if (submittedAnswer == null) {
            return false;
        }

        return quiz.getAnswer().trim().equalsIgnoreCase(submittedAnswer.trim());
    }

    private void syncQuizCompletion(QuizSyncRequest request) {
        try {
            quizSyncClient.sync(request);
        } catch (RuntimeException ex) {
            outboxService.recordQuizSyncFailure(request, syncErrorMessage(ex));
        }
    }

    private String syncErrorMessage(RuntimeException ex) {
        return SecuritySanitizer.safeErrorMessage(ex.getMessage(), "rust quiz sync gagal");
    }

    private void validateArticleId(String articleId) {
        if (articleId == null || articleId.isBlank()) {
            throw new BadRequestException("article_id wajib diisi");
        }
    }

    private void ensureArticleExists(String articleId) {
        if (!articleRepository.existsById(articleId)) {
            throw new NotFoundException("Artikel tidak ditemukan");
        }
    }
}
