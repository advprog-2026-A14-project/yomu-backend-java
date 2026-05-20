package id.ac.ui.cs.advprog.yomubackendjava.auth;

import id.ac.ui.cs.advprog.yomubackendjava.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {
    private static final String WEAK_PASSWORD_MESSAGE = "password terlalu lemah";
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;

    public void validateNewPassword(String password) {
        if (password == null
                || password.length() < MIN_LENGTH
                || password.length() > MAX_LENGTH
                || containsWhitespace(password)
                || !containsLetter(password)
                || !containsDigit(password)) {
            throw new BadRequestException(WEAK_PASSWORD_MESSAGE);
        }
    }

    private boolean containsWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsLetter(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isLetter(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsDigit(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isDigit(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
