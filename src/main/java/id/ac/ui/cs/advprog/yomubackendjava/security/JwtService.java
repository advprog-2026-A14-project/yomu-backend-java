package id.ac.ui.cs.advprog.yomubackendjava.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.UnauthorizedException;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.Role;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private static final int MIN_SECRET_LENGTH = 32;
    private static final String ROLE_CLAIM = "role";

    private final String secret;
    private final long ttlSeconds;
    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.secret = jwtProperties.secret();
        this.ttlSeconds = jwtProperties.ttlSeconds();
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("JWT secret must be at least 32 characters");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UUID userId, Role role) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(ttlSeconds);

        return Jwts.builder()
                .subject(userId.toString())
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public JwtClaims parseAndValidate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String subject = claims.getSubject();
            String roleValue = claims.get(ROLE_CLAIM, String.class);
            Date issuedAtDate = claims.getIssuedAt();
            Date expirationDate = claims.getExpiration();

            if (subject == null || roleValue == null || issuedAtDate == null || expirationDate == null) {
                throw new UnauthorizedException("Invalid token claims");
            }

            UUID userId = UUID.fromString(subject);
            Role role = Role.valueOf(roleValue);
            return new JwtClaims(userId, role, issuedAtDate.toInstant(), expirationDate.toInstant());
        } catch (IllegalArgumentException | JwtException ex) {
            throw new UnauthorizedException("Invalid or expired token");
        }
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
