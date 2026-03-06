package id.ac.ui.cs.advprog.yomubackendjava.auth;

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

import java.time.Instant;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginControllerTest {
    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String RAW_PASSWORD = "rahasia123";
    private static final String SUCCESS_JSON_PATH = "$.success";
    private static final String MESSAGE_JSON_PATH = "$.message";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void loginSuccessByUsernameShouldReturn200() throws Exception {
        saveActiveUser("login_username", "login.user@example.com", "+628121111111", true, false);

        mockMvc.perform(post(LOGIN_PATH)
                        .contentType(APPLICATION_JSON)
                .content("""
                                {
                                  "identifier": "login_username",
                                  "password": "rahasia123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath(SUCCESS_JSON_PATH).value(true))
                .andExpect(jsonPath("$.data.access_token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.role").value("PELAJAR"));
    }

    @Test
    void loginSuccessByEmailShouldReturn200() throws Exception {
        saveActiveUser("login_email", "login.email@example.com", "+628121111112", true, false);

        mockMvc.perform(post(LOGIN_PATH)
                        .contentType(APPLICATION_JSON)
                .content("""
                                {
                                  "identifier": "login.email@example.com",
                                  "password": "rahasia123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath(SUCCESS_JSON_PATH).value(true))
                .andExpect(jsonPath("$.data.access_token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("login.email@example.com"));
    }

    @Test
    void loginSuccessByPhoneShouldReturn200() throws Exception {
        saveActiveUser("login_phone", "login.phone@example.com", "+628121111113", true, false);

        mockMvc.perform(post(LOGIN_PATH)
                        .contentType(APPLICATION_JSON)
                .content("""
                                {
                                  "identifier": "+628121111113",
                                  "password": "rahasia123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath(SUCCESS_JSON_PATH).value(true))
                .andExpect(jsonPath("$.data.access_token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.phone_number").value("+628121111113"));
    }

    @Test
    void wrongPasswordShouldReturn401() throws Exception {
        saveActiveUser("wrong_password", "wrong.password@example.com", "+628121111114", true, false);

        mockMvc.perform(post(LOGIN_PATH)
                        .contentType(APPLICATION_JSON)
                .content("""
                                {
                                  "identifier": "wrong_password",
                                  "password": "salah_password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(SUCCESS_JSON_PATH).value(false))
                .andExpect(jsonPath(MESSAGE_JSON_PATH).value("identifier atau password salah"));
    }

    @Test
    void userNotFoundShouldReturn401() throws Exception {
        mockMvc.perform(post(LOGIN_PATH)
                        .contentType(APPLICATION_JSON)
                .content("""
                                {
                                  "identifier": "tidak_ada",
                                  "password": "rahasia123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(SUCCESS_JSON_PATH).value(false))
                .andExpect(jsonPath(MESSAGE_JSON_PATH).value("identifier atau password salah"));
    }

    @Test
    void deletedUserShouldReturn403() throws Exception {
        saveActiveUser("deleted_user", "deleted.user@example.com", "+628121111115", true, true);

        mockMvc.perform(post(LOGIN_PATH)
                        .contentType(APPLICATION_JSON)
                .content("""
                                {
                                  "identifier": "deleted_user",
                                  "password": "rahasia123"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(SUCCESS_JSON_PATH).value(false))
                .andExpect(jsonPath(MESSAGE_JSON_PATH).value("akun tidak aktif"));
    }

    @Test
    void ssoOnlyUserShouldReturn401() throws Exception {
        saveActiveUser("sso_user", "sso.user@example.com", "+628121111116", false, false);

        mockMvc.perform(post(LOGIN_PATH)
                        .contentType(APPLICATION_JSON)
                .content("""
                                {
                                  "identifier": "sso_user",
                                  "password": "rahasia123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(SUCCESS_JSON_PATH).value(false))
                .andExpect(jsonPath(MESSAGE_JSON_PATH).value("akun menggunakan metode login lain"));
    }

    private void saveActiveUser(
            String username,
            String email,
            String phoneNumber,
            boolean withPassword,
            boolean deleted
    ) {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setDisplayName("Display " + username);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setRole(Role.PELAJAR);
        user.setPasswordHash(withPassword ? passwordEncoder.encode(RAW_PASSWORD) : null);
        user.setDeletedAt(deleted ? Instant.now() : null);
        userRepository.saveAndFlush(user);
    }
}
