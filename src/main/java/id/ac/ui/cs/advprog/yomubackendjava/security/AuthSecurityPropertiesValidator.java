package id.ac.ui.cs.advprog.yomubackendjava.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Component
public class AuthSecurityPropertiesValidator implements ApplicationRunner {
    public static final String DEFAULT_JWT_SECRET =
            "ini_adalah_kunci_rahasia_yang_sangat_panjang_dan_aman_untuk_keperluan_testing_adpro";
    public static final String DEFAULT_INTERNAL_API_KEY = "any_random_key_for_internal_sync";
    private static final Set<String> STRICT_PROFILES = Set.of("prod", "production", "staging");

    private final Environment environment;

    public AuthSecurityPropertiesValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        validate();
    }

    public void validate() {
        if (!isStrictProfile()) {
            return;
        }
        rejectBlankOrDefault("jwt.secret", DEFAULT_JWT_SECRET, "JWT secret");
        rejectBlankOrDefault("internal.api.key", DEFAULT_INTERNAL_API_KEY, "Internal API key");
        rejectBlank("google.oauth.client-id", "Google OAuth client ID");
    }

    private boolean isStrictProfile() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch(STRICT_PROFILES::contains);
    }

    private void rejectBlankOrDefault(String propertyName, String defaultValue, String label) {
        String value = environment.getProperty(propertyName);
        rejectBlankValue(value, label);
        if (defaultValue.equals(value)) {
            throw new IllegalStateException(label + " must not use default value in strict profile");
        }
    }

    private void rejectBlank(String propertyName, String label) {
        rejectBlankValue(environment.getProperty(propertyName), label);
    }

    private void rejectBlankValue(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(label + " must not be blank in strict profile");
        }
    }
}
