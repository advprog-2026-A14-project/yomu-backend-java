package id.ac.ui.cs.advprog.yomubackendjava.user;

import id.ac.ui.cs.advprog.yomubackendjava.security.JwtAuthFilter;
import id.ac.ui.cs.advprog.yomubackendjava.security.JwtService;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.Role;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.UserEntity;
import id.ac.ui.cs.advprog.yomubackendjava.user.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UpdateAccountTest {
    private static final String ME_PATH = "/api/v1/users/me";
    private static final String ME_PASSWORD_PATH = "/api/v1/users/me/password";
    private static final String ME_IDENTIFIERS_PATH = "/api/v1/users/me/login-identifiers";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void updateDisplayNameShouldReturn200() throws Exception {
        UserEntity user = saveUser("upd_name", "Display Lama", "upd.name@example.com", null, "secret123");
        String token = tokenFor(user);

        mockMvc.perform(patch(ME_PATH)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "display_name": "Display Baru"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.display_name").value("Display Baru"));
    }

    @Test
    void updateUsernameConflictShouldReturn409() throws Exception {
        saveUser("username_exist", "Exist", "exist@example.com", null, "secret123");
        UserEntity user = saveUser("username_target", "Target", "target@example.com", null, "secret123");

        mockMvc.perform(patch(ME_PATH)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + tokenFor(user))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "username_exist"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void updatePasswordWithWrongCurrentShouldReturn401() throws Exception {
        UserEntity user = saveUser("pass_local", "Pass Local", "pass.local@example.com", null, "correct123");

        mockMvc.perform(patch(ME_PASSWORD_PATH)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + tokenFor(user))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "current_password": "salah",
                                  "new_password": "baru12345"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void setFirstPasswordForSsoUserShouldReturn200AndFillHash() throws Exception {
        UserEntity user = saveSsoOnlyUser("sso_first", "Sso First", "sso.first@example.com", null);

        mockMvc.perform(patch(ME_PASSWORD_PATH)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + tokenFor(user))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "new_password": "newSecure123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        UserEntity updated = userRepository.findById(user.getUserId()).orElseThrow();
        assertThat(updated.getPasswordHash()).isNotNull();
        assertThat(passwordEncoder.matches("newSecure123", updated.getPasswordHash())).isTrue();
    }

    @Test
    void addEmailShouldReturn200() throws Exception {
        UserEntity user = saveUser("add_email", "Add Email", null, "+628111223344", "secret123");

        mockMvc.perform(patch(ME_IDENTIFIERS_PATH)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + tokenFor(user))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "new.email@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("new.email@example.com"));
    }

    @Test
    void addPhoneShouldReturn200() throws Exception {
        UserEntity user = saveUser("add_phone", "Add Phone", "add.phone@example.com", null, "secret123");

        mockMvc.perform(patch(ME_IDENTIFIERS_PATH)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + tokenFor(user))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "phone_number": "+628222334455"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phone_number").value("+628222334455"));
    }

    @Test
    void identifierConflictShouldReturn409() throws Exception {
        saveUser("conflict_email_owner", "Owner", "owner@example.com", null, "secret123");
        UserEntity user = saveUser("conflict_email_target", "Target", null, "+628212111111", "secret123");

        mockMvc.perform(patch(ME_IDENTIFIERS_PATH)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + tokenFor(user))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "owner@example.com"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void emptyPayloadShouldReturn400() throws Exception {
        UserEntity user = saveUser("empty_payload", "Empty Payload", "empty.payload@example.com", null, "secret123");

        mockMvc.perform(patch(ME_PATH)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + tokenFor(user))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    private String tokenFor(UserEntity user) {
        return jwtService.generateToken(user.getUserId(), user.getRole());
    }

    private UserEntity saveUser(
            String username,
            String displayName,
            String email,
            String phoneNumber,
            String rawPassword
    ) {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(Role.PELAJAR);
        return userRepository.saveAndFlush(user);
    }

    private UserEntity saveSsoOnlyUser(
            String username,
            String displayName,
            String email,
            String phoneNumber
    ) {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setPasswordHash(null);
        user.setRole(Role.PELAJAR);
        return userRepository.saveAndFlush(user);
    }
}
