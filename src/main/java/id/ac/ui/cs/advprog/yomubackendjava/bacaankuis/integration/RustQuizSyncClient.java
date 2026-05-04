package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.integration;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizSyncRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class RustQuizSyncClient implements QuizSyncClient {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String syncUrl;
    private final String apiKey;

    public RustQuizSyncClient(
            @Value("${rust.engine.base-url}") String rustEngineBaseUrl,
            @Value("${internal.api.key}") String apiKey) {
        this.syncUrl = rustEngineBaseUrl + "/api/internal/quiz-history/sync";
        this.apiKey = apiKey;
    }

    @Override
    public void sync(QuizSyncRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<QuizSyncRequest> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(syncUrl, entity, String.class);
        } catch (RestClientException e) {
            System.err.println("Rust quiz sync failed: " + e.getMessage());
        }
    }
}
