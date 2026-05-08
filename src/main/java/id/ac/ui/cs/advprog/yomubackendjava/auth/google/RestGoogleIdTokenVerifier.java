package id.ac.ui.cs.advprog.yomubackendjava.auth.google;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
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
    private static final String INVALID_GOOGLE_RESPONSE_MESSAGE = "response verifikasi Google tidak valid";
    private static final Pattern STRING_FIELD_PATTERN_TEMPLATE = Pattern.compile("\"%s\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern NUMBER_FIELD_PATTERN_TEMPLATE = Pattern.compile("\"%s\"\\s*:\\s*\"?(\\d+)\"?");
    private static final Pattern BOOLEAN_FIELD_PATTERN_TEMPLATE = Pattern.compile("\"%s\"\\s*:\\s*\"?(true|false)\"?");

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
                        throw new IllegalArgumentException(INVALID_GOOGLE_RESPONSE_MESSAGE, ex);
                    }
                });

        GoogleTokenInfo tokenInfo = parseTokenInfo(body);
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

        String email = normalize(tokenInfo.email());
        String name = normalize(tokenInfo.name());
        return new GoogleProfile(subject, email, name);
    }

    private GoogleTokenInfo parseTokenInfo(String body) {
        return new GoogleTokenInfo(
                optionalStringField(body, "sub"),
                optionalStringField(body, "iss"),
                optionalStringField(body, "aud"),
                optionalNumberField(body, "exp"),
                optionalStringField(body, "email"),
                optionalBooleanField(body, "email_verified"),
                optionalStringField(body, "name")
        );
    }

    private String requiredStringField(String rawValue, String fieldName) {
        String value = normalize(rawValue);
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

    private Boolean optionalBooleanField(String body, String fieldName) {
        Pattern pattern = Pattern.compile(String.format(BOOLEAN_FIELD_PATTERN_TEMPLATE.pattern(), fieldName));
        Matcher matcher = pattern.matcher(body == null ? "" : body);
        if (!matcher.find()) {
            return null;
        }
        return Boolean.valueOf(matcher.group(1));
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
            String issuer,
            String audience,
            Long expiresAt,
            String email,
            Boolean emailVerified,
            String name
    ) {
    }
}
