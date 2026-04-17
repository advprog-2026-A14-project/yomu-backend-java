package id.ac.ui.cs.advprog.yomubackendjava.auth.google;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestGoogleIdTokenVerifierTest {
    private MockRestServiceServer server;
    private RestGoogleIdTokenVerifier verifier;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://oauth2.googleapis.com");
        server = MockRestServiceServer.bindTo(builder).build();
        verifier = new RestGoogleIdTokenVerifier(builder.build(), "test-client-id");
    }

    @Test
    void shouldVerifyValidToken() {
        server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=valid-token"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "sub": "google-sub",
                          "iss": "https://accounts.google.com",
                          "aud": "test-client-id",
                          "exp": "%d",
                          "email": "user@example.com",
                          "name": "Google User"
                        }
                        """.formatted(Instant.now().plusSeconds(300).getEpochSecond()), MediaType.APPLICATION_JSON));

        GoogleProfile profile = verifier.verify(" valid-token ");

        assertEquals("google-sub", profile.googleSub());
        assertEquals("user@example.com", profile.email());
        assertEquals("Google User", profile.name());
        server.verify();
    }

    @Test
    void shouldAllowMissingOptionalFieldsWhenAudienceNotConfigured() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://oauth2.googleapis.com");
        MockRestServiceServer localServer = MockRestServiceServer.bindTo(builder).build();
        RestGoogleIdTokenVerifier localVerifier = new RestGoogleIdTokenVerifier(builder.build(), " ");
        localServer.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=no-aud"))
                .andRespond(withSuccess("""
                        {
                          "sub": "google-sub",
                          "iss": "accounts.google.com"
                        }
                        """, MediaType.APPLICATION_JSON));

        GoogleProfile profile = localVerifier.verify("no-aud");

        assertEquals("google-sub", profile.googleSub());
        assertNull(profile.email());
        assertNull(profile.name());
        localServer.verify();
    }

    @Test
    void shouldRejectBlankToken() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> verifier.verify("   "));

        assertEquals("id_token kosong", exception.getMessage());
    }

    @Test
    void shouldRejectNonSuccessResponse() {
        server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=bad-token"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.BAD_REQUEST));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> verifier.verify("bad-token"));

        assertEquals("id_token tidak valid", exception.getMessage());
        server.verify();
    }

    @Test
    void shouldRejectMissingSubject() {
        server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=missing-sub"))
                .andRespond(withSuccess("""
                        {
                          "iss": "https://accounts.google.com",
                          "aud": "test-client-id"
                        }
                        """, MediaType.APPLICATION_JSON));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> verifier.verify("missing-sub"));

        assertEquals("sub tidak ditemukan", exception.getMessage());
        server.verify();
    }

    @Test
    void shouldRejectInvalidIssuer() {
        server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=invalid-issuer"))
                .andRespond(withSuccess("""
                        {
                          "sub": "google-sub",
                          "iss": "https://evil.example.com",
                          "aud": "test-client-id"
                        }
                        """, MediaType.APPLICATION_JSON));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> verifier.verify("invalid-issuer"));

        assertEquals("issuer id_token tidak valid", exception.getMessage());
        server.verify();
    }

    @Test
    void shouldRejectExpiredToken() {
        server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=expired-token"))
                .andRespond(withSuccess("""
                        {
                          "sub": "google-sub",
                          "iss": "https://accounts.google.com",
                          "aud": "test-client-id",
                          "exp": "%d"
                        }
                        """.formatted(Instant.now().minusSeconds(300).getEpochSecond()), MediaType.APPLICATION_JSON));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> verifier.verify("expired-token"));

        assertEquals("id_token expired", exception.getMessage());
        server.verify();
    }

    @Test
    void shouldRejectAudienceMismatch() {
        server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=wrong-aud"))
                .andRespond(withSuccess("""
                        {
                          "sub": "google-sub",
                          "iss": "https://accounts.google.com",
                          "aud": "another-client-id"
                        }
                        """, MediaType.APPLICATION_JSON));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> verifier.verify("wrong-aud"));

        assertEquals("audience id_token tidak valid", exception.getMessage());
        server.verify();
    }
}
