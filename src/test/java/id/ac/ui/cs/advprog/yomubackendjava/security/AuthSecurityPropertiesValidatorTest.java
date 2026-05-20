package id.ac.ui.cs.advprog.yomubackendjava.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthSecurityPropertiesValidatorTest {
    @Test
    void productionProfileShouldRejectDefaultJwtSecret() {
        MockEnvironment environment = productionEnvironment()
                .withProperty("jwt.secret", AuthSecurityPropertiesValidator.DEFAULT_JWT_SECRET)
                .withProperty("internal.api.key", "internal-api-key-yang-cukup-panjang")
                .withProperty("google.oauth.client-id", "client-id.apps.googleusercontent.com");

        assertThrows(IllegalStateException.class, () -> new AuthSecurityPropertiesValidator(environment).validate());
    }

    @Test
    void productionProfileShouldRejectBlankGoogleAudience() {
        MockEnvironment environment = productionEnvironment()
                .withProperty("jwt.secret", "jwt-secret-production-yang-cukup-panjang")
                .withProperty("internal.api.key", "internal-api-key-yang-cukup-panjang")
                .withProperty("google.oauth.client-id", "");

        assertThrows(IllegalStateException.class, () -> new AuthSecurityPropertiesValidator(environment).validate());
    }

    @Test
    void testProfileShouldAllowLocalDefaults() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.profiles.active", "test")
                .withProperty("jwt.secret", AuthSecurityPropertiesValidator.DEFAULT_JWT_SECRET)
                .withProperty("internal.api.key", AuthSecurityPropertiesValidator.DEFAULT_INTERNAL_API_KEY)
                .withProperty("google.oauth.client-id", "");

        assertDoesNotThrow(() -> new AuthSecurityPropertiesValidator(environment).validate());
    }

    private MockEnvironment productionEnvironment() {
        return new MockEnvironment().withProperty("spring.profiles.active", "prod");
    }
}
