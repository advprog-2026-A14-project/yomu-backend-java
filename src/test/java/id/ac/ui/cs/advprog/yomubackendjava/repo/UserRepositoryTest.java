package id.ac.ui.cs.advprog.yomubackendjava.repo;

import id.ac.ui.cs.advprog.yomubackendjava.user.domain.Role;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.UserEntity;
import id.ac.ui.cs.advprog.yomubackendjava.user.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    void insertUserSuccess() {
        UserEntity user = buildUser("pelajar01", "pelajar01@example.com");
        UserEntity saved = userRepository.saveAndFlush(user);

        assertThat(saved.getUserId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void usernameUniqueConstraintShouldRejectDuplicate() {
        UserEntity first = buildUser("duplicate-user", "dup1@example.com");
        UserEntity second = buildUser("duplicate-user", "dup2@example.com");

        userRepository.saveAndFlush(first);

        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(second));
    }

    @Test
    void emailUniqueConstraintShouldRejectDuplicate() {
        UserEntity first = buildUser("first-user", "same@example.com");
        UserEntity second = buildUser("second-user", "same@example.com");

        userRepository.saveAndFlush(first);

        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(second));
    }

    @Test
    void softDeletedUserShouldNotBeReturnedByActiveQuery() {
        UserEntity user = buildUser("soft-delete-user", "soft@example.com");
        UserEntity saved = userRepository.saveAndFlush(user);

        saved.setDeletedAt(Instant.now());
        userRepository.saveAndFlush(saved);

        assertThat(userRepository.findByUsernameAndDeletedAtIsNull("soft-delete-user")).isEmpty();
    }

    private UserEntity buildUser(String username, String email) {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setDisplayName("Display " + username);
        user.setEmail(email);
        user.setPhoneNumber("+6281234567890" + username.hashCode());
        user.setPasswordHash("hash");
        user.setRole(Role.PELAJAR);
        user.setGoogleSub("google-sub-" + username);
        return user;
    }
}
