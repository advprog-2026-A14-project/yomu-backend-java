package id.ac.ui.cs.advprog.yomubackendjava.security;

import id.ac.ui.cs.advprog.yomubackendjava.auth.AuthEventLogger;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.ForbiddenException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.UnauthorizedException;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.UserEntity;
import id.ac.ui.cs.advprog.yomubackendjava.user.repo.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserStateValidator {
    private static final String USER_DELETED_REASON = "USER_DELETED";
    private static final String STALE_TOKEN_REASON = "STALE_TOKEN";

    private final UserRepository userRepository;
    private final AuthEventLogger authEventLogger;

    public AuthenticatedUserStateValidator(UserRepository userRepository, AuthEventLogger authEventLogger) {
        this.userRepository = userRepository;
        this.authEventLogger = authEventLogger;
    }

    public void validate(JwtService.JwtClaims claims) {
        userRepository.findById(claims.userId()).ifPresent(user -> validateKnownUser(user, claims));
    }

    private void validateKnownUser(UserEntity user, JwtService.JwtClaims claims) {
        if (user.getDeletedAt() != null) {
            authEventLogger.jwtValidationFailed(USER_DELETED_REASON);
            throw new ForbiddenException("akun tidak aktif");
        }
        if (user.getTokenVersion() != claims.tokenVersion()) {
            authEventLogger.jwtValidationFailed(STALE_TOKEN_REASON);
            throw new UnauthorizedException("Invalid or expired token");
        }
    }
}
