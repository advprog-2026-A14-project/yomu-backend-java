package id.ac.ui.cs.advprog.yomubackendjava.integration.rust;

import id.ac.ui.cs.advprog.yomubackendjava.integration.rust.dto.SyncUserRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

@Component
public class RestClientRustEngineClient implements RustEngineClient {
    private static final String USER_SYNC_ENDPOINT = "/api/internal/users/sync";
    private static final String API_KEY_HEADER = "x-api-key";

    private final RestClient restClient;

    public RestClientRustEngineClient(
            @Value("${rust.engine.base-url:http://localhost:8080}") String baseUrl,
            @Value("${internal.api.key:}") String internalApiKey
    ) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(2))
                        .build()
        );
        requestFactory.setReadTimeout(Duration.ofSeconds(3));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(API_KEY_HEADER, internalApiKey)
                .build();
    }

    @Override
    public SyncResult syncUser(UUID userId) {
        return restClient.post()
                .uri(USER_SYNC_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SyncUserRequest(userId.toString()))
                .exchange((request, response) -> {
                    int statusCode = response.getStatusCode().value();
                    String responseBody = "";
                    if (response.getBody() != null) {
                        try {
                            responseBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                        } catch (IOException ignored) {
                            responseBody = "";
                        }
                    }
                    return new SyncResult(statusCode, responseBody);
                });
    }
}
