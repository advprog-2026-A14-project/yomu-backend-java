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
    private static final int MAX_TOKEN_LENGTH = 4096;
    private static final String ROLE_CLAIM = "role";
    private static final String TOKEN_VERSION_CLAIM = "token_version";

    private final long ttlSeconds;
    private final String issuer;
    private final String audience;
    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        String secret = jwtProperties.secret();
        this.ttlSeconds = jwtProperties.ttlSeconds();
        this.issuer = requireConfig(jwtProperties.issuer(), "JWT issuer must not be blank");
        this.audience = requireConfig(jwtProperties.audience(), "JWT audience must not be blank");
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
                .issuer(issuer)
                .audience().add(audience).and()
                .claim(ROLE_CLAIM, role.name())
                .claim(TOKEN_VERSION_CLAIM, 0)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public String generateToken(UUID userId, Role role, int tokenVersion) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(ttlSeconds);

        return Jwts.builder()
                .subject(userId.toString())
                .issuer(issuer)
                .audience().add(audience).and()
                .claim(ROLE_CLAIM, role.name())
                .claim(TOKEN_VERSION_CLAIM, tokenVersion)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public JwtClaims parseAndValidate(String token) {
        if (token == null || token.isBlank() || token.length() > MAX_TOKEN_LENGTH) {
            throw new UnauthorizedException("Invalid or expired token");
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String subject = claims.getSubject();
            String roleValue = claims.get(ROLE_CLAIM, String.class);
            Integer tokenVersion = claims.get(TOKEN_VERSION_CLAIM, Integer.class);
            Date issuedAtDate = claims.getIssuedAt();
            Date expirationDate = claims.getExpiration();

            if (subject == null || roleValue == null || issuedAtDate == null || expirationDate == null) {
                throw new UnauthorizedException("Invalid token claims");
            }

            UUID userId = UUID.fromString(subject);
            Role role = Role.valueOf(roleValue);
            return new JwtClaims(userId, role, issuedAtDate.toInstant(), expirationDate.toInstant(), tokenVersion == null ? 0 : tokenVersion);
        } catch (IllegalArgumentException | JwtException ex) {
            throw new UnauthorizedException("Invalid or expired token");
        }
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    private String requireConfig(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    public record JwtClaims(UUID userId, Role role, Instant issuedAt, Instant expiresAt, int tokenVersion) {
        public JwtClaims(UUID userId, Role role, Instant issuedAt, Instant expiresAt) {
            this(userId, role, issuedAt, expiresAt, 0);
        }
    }
}
