package id.ac.ui.cs.advprog.yomubackendjava.integration.rust;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;

@Component
public class RestClientRustLeagueClient implements RustLeagueClient {
    private static final String USER_TIER_ENDPOINT = "/api/v1/league/users/{userId}/tier";
    private static final String API_KEY_HEADER = "x-api-key";

    private final RestClient restClient;

    public RestClientRustLeagueClient(
            @Value("${rust.engine.base-url:http://localhost:8080}") String baseUrl,
            @Value("${internal.api.key:}") String internalApiKey
    ) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(2))
                        .build()
        );
        factory.setReadTimeout(Duration.ofSeconds(3));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(API_KEY_HEADER, internalApiKey)
                .build();
    }

    RestClientRustLeagueClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public UserTierResponse getUserTier(UUID userId) {
        ApiWrapper wrapper = restClient.get()
                .uri(USER_TIER_ENDPOINT, userId)
                .retrieve()
                .body(RustLeagueClient.ApiWrapper.class);
        
        return wrapper != null ? wrapper.data() : null;
    }
}