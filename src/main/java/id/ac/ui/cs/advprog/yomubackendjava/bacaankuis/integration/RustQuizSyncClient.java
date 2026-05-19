package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.integration;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizSyncRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@ConditionalOnProperty(name = "rust.integration.transport", havingValue = "rest")
public class RustQuizSyncClient implements QuizSyncClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(RustQuizSyncClient.class);
    private static final String SYNC_ENDPOINT = "/api/internal/quiz-history/sync";
    private static final String API_KEY_HEADER = "x-api-key";

    private final RestClient restClient;

    public RustQuizSyncClient(
            @Value("${rust.engine.base-url}") String rustEngineBaseUrl,
            @Value("${internal.api.key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(rustEngineBaseUrl)
                .defaultHeader(API_KEY_HEADER, apiKey)
                .build();
    }

    @Override
    public void sync(QuizSyncRequest request) {
        try {
            restClient.post()
                    .uri(SYNC_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            LOGGER.warn("Rust quiz REST sync failed: {}", e.getMessage());
            throw e;
        }
    }
}
