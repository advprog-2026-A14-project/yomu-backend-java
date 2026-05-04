package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizSyncRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.UserAttempt;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.UserAttemptRepository;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.BadRequestException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.ConflictException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Service
public class QuizService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final UserAttemptRepository attemptRepository;

    @Value("${internal.api.key:}")
    private String apiKey;

    public QuizService(UserAttemptRepository attemptRepository) {
        this.attemptRepository = attemptRepository;
    }

    public void syncToRust(QuizSyncRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<QuizSyncRequest> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity("http://rust-engine:8080/api/internal/quiz-history/sync", entity, String.class);
        } catch (Exception e) {
            System.err.println("Rust Sync Failed: " + e.getMessage());
        }
    }

    public void submitAndSync(QuizSyncRequest request) {
        validateRequest(request);

        if (attemptRepository.existsByUserIdAndKuisId(request.getUserId(), request.getArticleId())) {
            throw new ConflictException("Kuis sudah pernah dikerjakan!");
        }

        UserAttempt attempt = new UserAttempt();
        attempt.setUserId(request.getUserId());
        attempt.setKuisId(request.getArticleId());
        attempt.setCompletedAt(LocalDateTime.now());
        attemptRepository.save(attempt);

        syncToRust(request);
    }

    private void validateRequest(QuizSyncRequest request) {
        if (request == null) {
            throw new BadRequestException("Request kuis tidak boleh kosong");
        }
        if (request.getUserId() == null) {
            throw new BadRequestException("user_id wajib diisi");
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
