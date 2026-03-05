package id.ac.ui.cs.advprog.yomubackendjava.auth;

import id.ac.ui.cs.advprog.yomubackendjava.user.repo.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class UsernameGenerator {
    private static final String DEFAULT_USERNAME_BASE = "google_user";

    private final UserRepository userRepository;

    public UsernameGenerator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String generateFromEmail(String email) {
        String base = DEFAULT_USERNAME_BASE;
        if (email != null && email.contains("@")) {
            base = email.substring(0, email.indexOf('@'));
        }

        String normalizedBase = normalizeBase(base);
        String candidate = normalizedBase;
        int suffix = 1;
        while (userRepository.findByUsername(candidate).isPresent()) {
            candidate = normalizedBase + suffix;
            suffix++;
        }
        return candidate;
    }

    private String normalizeBase(String rawBase) {
        String base = rawBase == null ? DEFAULT_USERNAME_BASE : rawBase.toLowerCase();
        String sanitized = base.replaceAll("[^a-z0-9_]", "_");
        String compact = sanitized.replaceAll("_+", "_");
        String trimmed = compact.replaceAll("^_+|_+$", "");
        return trimmed.isBlank() ? DEFAULT_USERNAME_BASE : trimmed;
    }
}
