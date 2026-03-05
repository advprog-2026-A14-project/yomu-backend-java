package id.ac.ui.cs.advprog.yomubackendjava.security;

import id.ac.ui.cs.advprog.yomubackendjava.common.exception.UnauthorizedException;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class JwtService {
    private final String secret;
    private final long ttlSeconds;

    public JwtService(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.ttl-seconds:86400}") long ttlSeconds
    ) {
        this.secret = secret;
        this.ttlSeconds = ttlSeconds;
    }

    public String generateToken(UUID userId, Role role) {
        return "stub-token";
    }

    public JwtClaims parseAndValidate(String token) {
        throw new UnauthorizedException("Invalid token");
    }

    public String getSecret() {
        return secret;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public record JwtClaims(UUID userId, Role role, Instant issuedAt, Instant expiresAt) {
    }
}
