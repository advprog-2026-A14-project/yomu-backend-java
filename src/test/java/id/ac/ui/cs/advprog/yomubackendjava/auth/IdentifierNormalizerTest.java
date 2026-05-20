package id.ac.ui.cs.advprog.yomubackendjava.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IdentifierNormalizerTest {
    private final IdentifierNormalizer identifierNormalizer = new IdentifierNormalizer();

    @Test
    void emailShouldBeTrimmedAndLowercased() {
        assertEquals("user@example.com", identifierNormalizer.email("  User@Example.COM  "));
    }

    @Test
    void usernameShouldBeTrimmedAndLowercased() {
        assertEquals("pelajar_01", identifierNormalizer.username("  Pelajar_01  "));
    }

    @Test
    void phoneShouldRemoveCommonSeparators() {
        assertEquals("+628123456789", identifierNormalizer.phoneNumber(" +62 812-345.6789 "));
    }

    @Test
    void blankValueShouldNormalizeToNull() {
        assertNull(identifierNormalizer.email("   "));
    }
}
