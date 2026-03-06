package id.ac.ui.cs.advprog.yomubackendjava.auth;

import org.springframework.stereotype.Component;

@Component
public class IdentifierResolver {
    public ResolvedIdentifier resolve(String identifier) {
        if (identifier.contains("@")) {
            return new ResolvedIdentifier(IdentifierType.EMAIL, identifier);
        }
        if (identifier.startsWith("+") || isDigitsOnly(identifier)) {
            return new ResolvedIdentifier(IdentifierType.PHONE_NUMBER, identifier);
        }
        return new ResolvedIdentifier(IdentifierType.USERNAME, identifier);
    }

    private boolean isDigitsOnly(String identifier) {
        for (int i = 0; i < identifier.length(); i++) {
            if (!Character.isDigit(identifier.charAt(i))) {
                return false;
            }
        }
        return !identifier.isEmpty();
    }

    public record ResolvedIdentifier(IdentifierType type, String value) {
    }

    public enum IdentifierType {
        USERNAME,
        EMAIL,
        PHONE_NUMBER
    }
}
