package id.ac.ui.cs.advprog.yomubackendjava.common.security;

import org.springframework.web.util.HtmlUtils;

import java.util.regex.Pattern;

public final class SecuritySanitizer {
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}&&[^\r\n\t]]");
    private static final int MAX_ERROR_MESSAGE_LENGTH = 200;

    private SecuritySanitizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = CONTROL_CHARS.matcher(value.trim()).replaceAll("");
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String html(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : HtmlUtils.htmlEscape(normalized);
    }

    public static String safeErrorMessage(String message, String fallback) {
        String normalized = normalize(message);
        String safeMessage = normalized == null ? fallback : normalized;
        safeMessage = safeMessage.replaceAll("[\\r\\n\\t]+", " ");
        safeMessage = HtmlUtils.htmlEscape(safeMessage);
        if (safeMessage.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return safeMessage;
        }
        return safeMessage.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}
