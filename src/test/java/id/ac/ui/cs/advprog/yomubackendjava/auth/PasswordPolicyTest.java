package id.ac.ui.cs.advprog.yomubackendjava.auth;

import id.ac.ui.cs.advprog.yomubackendjava.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {
    private final PasswordPolicy passwordPolicy = new PasswordPolicy();

    @Test
    void strongEnoughPasswordShouldPass() {
        assertDoesNotThrow(() -> passwordPolicy.validateNewPassword("rahasia123"));
    }

    @Test
    void passwordWithoutDigitShouldBeRejected() {
        assertThrows(BadRequestException.class, () -> passwordPolicy.validateNewPassword("rahasiaku"));
    }

    @Test
    void passwordWithoutLetterShouldBeRejected() {
        assertThrows(BadRequestException.class, () -> passwordPolicy.validateNewPassword("12345678"));
    }

    @Test
    void passwordWithWhitespaceShouldBeRejected() {
        assertThrows(BadRequestException.class, () -> passwordPolicy.validateNewPassword("rahasia 123"));
    }
}
