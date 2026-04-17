package id.ac.ui.cs.advprog.yomubackendjava.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IdentifierResolverTest {
    private final IdentifierResolver identifierResolver = new IdentifierResolver();

    @Test
    void shouldResolveEmail() {
        IdentifierResolver.ResolvedIdentifier result = identifierResolver.resolve("user@example.com");

        assertEquals(IdentifierResolver.IdentifierType.EMAIL, result.type());
        assertEquals("user@example.com", result.value());
    }

    @Test
    void shouldResolvePhoneNumberWithPlusPrefix() {
        IdentifierResolver.ResolvedIdentifier result = identifierResolver.resolve("+628123456789");

        assertEquals(IdentifierResolver.IdentifierType.PHONE_NUMBER, result.type());
    }

    @Test
    void shouldResolvePhoneNumberWhenDigitsOnly() {
        IdentifierResolver.ResolvedIdentifier result = identifierResolver.resolve("08123456789");

        assertEquals(IdentifierResolver.IdentifierType.PHONE_NUMBER, result.type());
    }

    @Test
    void shouldResolveUsernameWhenNotEmailOrPhoneNumber() {
        IdentifierResolver.ResolvedIdentifier result = identifierResolver.resolve("user.name-01");

        assertEquals(IdentifierResolver.IdentifierType.USERNAME, result.type());
    }

    @Test
    void shouldResolveEmptyValueAsUsername() {
        IdentifierResolver.ResolvedIdentifier result = identifierResolver.resolve("");

        assertEquals(IdentifierResolver.IdentifierType.USERNAME, result.type());
        assertEquals("", result.value());
    }
}
