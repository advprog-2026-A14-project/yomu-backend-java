package id.ac.ui.cs.advprog.yomubackendjava.auth.google;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Component
public class RestGoogleIdTokenVerifier implements GoogleIdTokenVerifier {
    private static final Set<String> ALLOWED_ISSUERS = Set.of(
            "https://accounts.google.com",
            "accounts.google.com"
    );
    private static final String INVALID_GOOGLE_RESPONSE_MESSAGE = "response verifikasi Google tidak valid";

    private final RestClient restClient;
    private final String expectedAudience;

    @Autowired
    public RestGoogleIdTokenVerifier(@Value("${google.oauth.client-id:}") String expectedAudience) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(2))
                        .build()
        );
        requestFactory.setReadTimeout(Duration.ofSeconds(3));

        this.restClient = RestClient.builder()
                .baseUrl("https://oauth2.googleapis.com")
                .requestFactory(requestFactory)
                .build();
        this.expectedAudience = normalize(expectedAudience);
    }

    RestGoogleIdTokenVerifier(RestClient restClient, String expectedAudience) {
        this.restClient = restClient;
        this.expectedAudience = normalize(expectedAudience);
    }

    @Override
    public GoogleProfile verify(String idToken) {
        String normalizedToken = normalize(idToken);
        if (normalizedToken == null) {
            throw new IllegalArgumentException("id_token kosong");
        }

        GoogleTokenInfo tokenInfo = fetchTokenInfo(normalizedToken);
        String subject = validateTokenInfo(tokenInfo);

        String email = normalize(tokenInfo.email());
        String name = normalize(tokenInfo.name());
        return new GoogleProfile(subject, email, name);
    }

    private GoogleTokenInfo fetchTokenInfo(String normalizedToken) {
        try {
            GoogleTokenInfo tokenInfo = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tokeninfo")
                        .queryParam("id_token", normalizedToken)
                        .build())
                .retrieve()
                .body(GoogleTokenInfo.class);
            if (tokenInfo == null) {
                throw new IllegalArgumentException(INVALID_GOOGLE_RESPONSE_MESSAGE);
            }
            return tokenInfo;
        } catch (RestClientResponseException ex) {
            throw new IllegalArgumentException("id_token tidak valid");
        } catch (RestClientException ex) {
            throw new IllegalArgumentException(INVALID_GOOGLE_RESPONSE_MESSAGE, ex);
        }
    }

    private String validateTokenInfo(GoogleTokenInfo tokenInfo) {
        String subject = requiredStringField(tokenInfo.sub(), "sub");
        String issuer = requiredStringField(tokenInfo.issuer(), "iss");
        if (!ALLOWED_ISSUERS.contains(issuer)) {
            throw new IllegalArgumentException("issuer id_token tidak valid");
        }

        Long exp = tokenInfo.expiresAt();
        if (exp != null && Instant.ofEpochSecond(exp).isBefore(Instant.now())) {
            throw new IllegalArgumentException("id_token expired");
        }

        String audience = normalize(tokenInfo.audience());
        if (expectedAudience != null) {
            if (audience == null || !expectedAudience.equals(audience)) {
                throw new IllegalArgumentException("audience id_token tidak valid");
            }
        }

        if (Boolean.FALSE.equals(tokenInfo.emailVerified())) {
            throw new IllegalArgumentException("email id_token belum terverifikasi");
        }
        return subject;
    }

    private String requiredStringField(String rawValue, String fieldName) {
        String value = normalize(rawValue);
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " tidak ditemukan");
        }
        return value;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record GoogleTokenInfo(
            String sub,
            @JsonProperty("iss") String issuer,
            @JsonProperty("aud") String audience,
            @JsonProperty("exp") Long expiresAt,
            String email,
            @JsonProperty("email_verified") Boolean emailVerified,
            String name
    ) {
    }
}
