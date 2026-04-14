package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizSyncRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.QuizCompletionEvent;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.UserAttemptRepository;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.UserAttempt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Service
public class QuizService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final UserAttemptRepository attemptRepository;

    @Value("${INTERNAL_API_KEY}")
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
        // Validasi aturan hanya one take
        if (attemptRepository.existsByUserIdAndKuisId(request.getUserId(), request.getArticleId())) {
            throw new IllegalStateException("Kuis sudah pernah dikerjakan!");
        }

        // Simpan progress lokal
        UserAttempt attempt = new UserAttempt();
        attempt.setUserId(request.getUserId());
        attempt.setKuisId(request.getArticleId());
        attempt.setCompletedAt(LocalDateTime.now());
        attemptRepository.save(attempt);

        syncToRust(request);
    }
}