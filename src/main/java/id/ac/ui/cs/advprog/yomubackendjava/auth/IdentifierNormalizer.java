package id.ac.ui.cs.advprog.yomubackendjava.auth;

import id.ac.ui.cs.advprog.yomubackendjava.common.security.SecuritySanitizer;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class IdentifierNormalizer {
    public String username(String value) {
        String normalized = SecuritySanitizer.normalize(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    public String email(String value) {
        String normalized = SecuritySanitizer.normalize(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    public String phoneNumber(String value) {
        String normalized = SecuritySanitizer.normalize(value);
        return normalized == null ? null : normalized.replaceAll("[ .-]", "");
    }

    public String loginIdentifier(String value) {
        String normalized = SecuritySanitizer.normalize(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.contains("@")) {
            return email(normalized);
        }
        if (normalized.startsWith("+") || digitsOrPhoneSeparatorsOnly(normalized)) {
            return phoneNumber(normalized);
        }
        return username(normalized);
    }

    private boolean digitsOrPhoneSeparatorsOnly(String value) {
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (!Character.isDigit(current) && current != ' ' && current != '.' && current != '-') {
                return false;
            }
        }
        return !value.isBlank();
    }
}
