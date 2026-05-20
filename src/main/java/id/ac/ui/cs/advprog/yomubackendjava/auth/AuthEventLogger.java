package id.ac.ui.cs.advprog.yomubackendjava.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuthEventLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthEventLogger.class);

    public void registerSuccess(UUID userId) {
        LOGGER.info("event=auth_register_success user_id={}", userId);
    }

    public void loginSuccess(UUID userId) {
        LOGGER.info("event=auth_login_success user_id={}", userId);
    }

    public void loginFailed(String reasonCode) {
        LOGGER.warn("event=auth_login_failed reason={}", reasonCode);
    }

    public void googleLoginFailed(String reasonCode) {
        LOGGER.warn("event=auth_google_login_failed reason={}", reasonCode);
    }

    public void passwordChanged(UUID userId) {
        LOGGER.info("event=auth_password_changed user_id={}", userId);
    }

    public void jwtValidationFailed(String reasonCode) {
        LOGGER.warn("event=auth_jwt_validation_failed reason={}", reasonCode);
    }
}
