package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizSyncRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

@Service
public class QuizService {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${internal.api.key}")
    private String apiKey;

    public void submitAndSync(QuizSyncRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<QuizSyncRequest> entity = new HttpEntity<>(request, headers);
            // Sync result to Rust Engine
            restTemplate.postForEntity("http://rust-engine:8080/api/internal/quiz-history/sync", entity, String.class);
        } catch (Exception e) {
            // Fault Tolerance: Java stays alive if Rust is down
            System.err.println("Rust Sync Failed: " + e.getMessage());
        }
    }
}