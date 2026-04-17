package id.ac.ui.cs.advprog.yomubackendjava.auth;

import id.ac.ui.cs.advprog.yomubackendjava.user.repo.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsernameGeneratorTest {
    private static final String DEFAULT_GOOGLE_USERNAME = "google_user";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UsernameGenerator usernameGenerator = new UsernameGenerator(userRepository);

    @Test
    void shouldUseEmailPrefixWhenAvailable() {
        when(userRepository.findByUsername("google.user")).thenReturn(Optional.empty());

        String result = usernameGenerator.generateFromEmail("google.user@example.com");

        assertEquals(DEFAULT_GOOGLE_USERNAME, result);
    }

    @Test
    void shouldAppendSuffixWhenUsernameAlreadyExists() {
        when(userRepository.findByUsername("taken_name")).thenReturn(Optional.of(mock()));
        when(userRepository.findByUsername("taken_name1")).thenReturn(Optional.empty());

        String result = usernameGenerator.generateFromEmail("taken.name@example.com");

        assertEquals("taken_name1", result);
    }

    @Test
    void shouldUseDefaultBaseWhenEmailMissing() {
        when(userRepository.findByUsername(DEFAULT_GOOGLE_USERNAME)).thenReturn(Optional.empty());

        String result = usernameGenerator.generateFromEmail(null);

        assertEquals(DEFAULT_GOOGLE_USERNAME, result);
    }

    @Test
    void shouldFallbackToDefaultBaseWhenPrefixSanitizedToBlank() {
        when(userRepository.findByUsername(DEFAULT_GOOGLE_USERNAME)).thenReturn(Optional.empty());

        String result = usernameGenerator.generateFromEmail("!!!@example.com");

        assertEquals(DEFAULT_GOOGLE_USERNAME, result);
    }
}
