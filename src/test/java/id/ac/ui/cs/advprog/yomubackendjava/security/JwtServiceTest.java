package id.ac.ui.cs.advprog.yomubackendjava.security;

import id.ac.ui.cs.advprog.yomubackendjava.common.exception.UnauthorizedException;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.Role;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {
    private static final String TEST_SECRET = "01234567890123456789012345678901";

    @Test
    void generateThenParseShouldKeepSubjectAndRole() {
        JwtService jwtService = new JwtService(new JwtProperties(TEST_SECRET, 3600));
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateToken(userId, Role.PELAJAR);
        JwtService.JwtClaims claims = jwtService.parseAndValidate(token);

        assertThat(claims.userId()).isEqualTo(userId);
        assertThat(claims.role()).isEqualTo(Role.PELAJAR);
    }

    @Test
    void generatedTokenShouldHaveExpAfterIat() {
        JwtService jwtService = new JwtService(new JwtProperties(TEST_SECRET, 3600));

        JwtService.JwtClaims claims = jwtService.parseAndValidate(
                jwtService.generateToken(UUID.randomUUID(), Role.ADMIN)
        );

        assertThat(claims.expiresAt()).isAfter(claims.issuedAt());
    }

    @Test
    void expiredTokenShouldBeRejected() {
        JwtService jwtService = new JwtService(new JwtProperties(TEST_SECRET, -1));

        String token = jwtService.generateToken(UUID.randomUUID(), Role.PELAJAR);

        assertThrows(UnauthorizedException.class, () -> jwtService.parseAndValidate(token));
    }
}
