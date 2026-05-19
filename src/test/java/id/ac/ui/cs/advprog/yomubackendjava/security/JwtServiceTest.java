package id.ac.ui.cs.advprog.yomubackendjava.security;

import id.ac.ui.cs.advprog.yomubackendjava.common.exception.UnauthorizedException;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.Role;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {
    private static final String TEST_SECRET = "01234567890123456789012345678901";
    private static final String TEST_ISSUER = "yomu-backend-java";
    private static final String TEST_AUDIENCE = "yomu-clients";

    @Test
    void generateThenParseShouldKeepSubjectAndRole() {
        JwtService jwtService = jwtService(3600);
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateToken(userId, Role.PELAJAR);
        JwtService.JwtClaims claims = jwtService.parseAndValidate(token);

        assertThat(claims.userId()).isEqualTo(userId);
        assertThat(claims.role()).isEqualTo(Role.PELAJAR);
    }

    @Test
    void generatedTokenShouldHaveExpAfterIat() {
        JwtService jwtService = jwtService(3600);

        JwtService.JwtClaims claims = jwtService.parseAndValidate(
                jwtService.generateToken(UUID.randomUUID(), Role.ADMIN)
        );

        assertThat(claims.expiresAt()).isAfter(claims.issuedAt());
    }

    @Test
    void expiredTokenShouldBeRejected() {
        JwtService jwtService = jwtService(-1);

        String token = jwtService.generateToken(UUID.randomUUID(), Role.PELAJAR);

        assertThrows(UnauthorizedException.class, () -> jwtService.parseAndValidate(token));
    }

    @Test
    void tokenWithWrongIssuerShouldBeRejected() {
        JwtService issuerA = new JwtService(new JwtProperties(TEST_SECRET, 3600, "issuer-a", "yomu-clients"));
        JwtService issuerB = new JwtService(new JwtProperties(TEST_SECRET, 3600, "issuer-b", "yomu-clients"));

        String token = issuerA.generateToken(UUID.randomUUID(), Role.PELAJAR);

        assertThrows(UnauthorizedException.class, () -> issuerB.parseAndValidate(token));
    }

    @Test
    void oversizedTokenShouldBeRejectedBeforeParsing() {
        JwtService jwtService = jwtService(3600);
        String oversizedToken = "a".repeat(4097);

        assertThrows(UnauthorizedException.class, () -> jwtService.parseAndValidate(oversizedToken));
    }

    private JwtService jwtService(long ttlSeconds) {
        return new JwtService(new JwtProperties(TEST_SECRET, ttlSeconds, TEST_ISSUER, TEST_AUDIENCE));
    }
}
