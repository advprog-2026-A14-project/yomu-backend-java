package id.ac.ui.cs.advprog.yomubackendjava.auth.google;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RestGoogleIdTokenVerifier implements GoogleIdTokenVerifier {
    private static final Set<String> ALLOWED_ISSUERS = Set.of(
            "https://accounts.google.com",
            "accounts.google.com"
    );
    private static final Pattern STRING_FIELD_PATTERN_TEMPLATE = Pattern.compile("\"%s\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern NUMBER_FIELD_PATTERN_TEMPLATE = Pattern.compile("\"%s\"\\s*:\\s*\"?(\\d+)\"?");

    private final RestClient restClient;
    private final String expectedAudience;

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

    @Override
    public GoogleProfile verify(String idToken) {
        String normalizedToken = normalize(idToken);
        if (normalizedToken == null) {
            throw new IllegalArgumentException("id_token kosong");
        }

        String body = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tokeninfo")
                        .queryParam("id_token", normalizedToken)
                        .build())
                .exchange((request, response) -> {
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new IllegalArgumentException("id_token tidak valid");
                    }
                    if (response.getBody() == null) {
                        return "";
                    }
                    try {
                        return StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                    } catch (IOException ex) {
                        throw new IllegalArgumentException("response verifikasi Google tidak valid", ex);
                    }
                });

        String subject = requiredStringField(body, "sub");
        String issuer = requiredStringField(body, "iss");
        if (!ALLOWED_ISSUERS.contains(issuer)) {
            throw new IllegalArgumentException("issuer id_token tidak valid");
        }

        Long exp = optionalNumberField(body, "exp");
        if (exp != null && Instant.ofEpochSecond(exp).isBefore(Instant.now())) {
            throw new IllegalArgumentException("id_token expired");
        }

        String audience = optionalStringField(body, "aud");
        if (expectedAudience != null) {
            if (audience == null || !expectedAudience.equals(audience)) {
                throw new IllegalArgumentException("audience id_token tidak valid");
            }
        }

        String email = optionalStringField(body, "email");
        String name = optionalStringField(body, "name");
        return new GoogleProfile(subject, email, name);
    }

    private String requiredStringField(String body, String fieldName) {
        String value = optionalStringField(body, fieldName);
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " tidak ditemukan");
        }
        return value;
    }

    private String optionalStringField(String body, String fieldName) {
        Pattern pattern = Pattern.compile(String.format(STRING_FIELD_PATTERN_TEMPLATE.pattern(), fieldName));
        Matcher matcher = pattern.matcher(body == null ? "" : body);
        if (!matcher.find()) {
            return null;
        }
        return normalize(matcher.group(1));
    }

    private Long optionalNumberField(String body, String fieldName) {
        Pattern pattern = Pattern.compile(String.format(NUMBER_FIELD_PATTERN_TEMPLATE.pattern(), fieldName));
        Matcher matcher = pattern.matcher(body == null ? "" : body);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " tidak valid");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
