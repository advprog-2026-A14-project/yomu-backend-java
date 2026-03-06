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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeleteAccountTest {
    private static final String DELETE_ME_PATH = "/api/v1/users/me";
    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String RAW_PASSWORD = "delete123";

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
    void deleteAccountShouldReturn200AndSoftDeleteUser() throws Exception {
        UserEntity user = saveActiveUser("delete_me", "delete.me@example.com");

        mockMvc.perform(delete(DELETE_ME_PATH)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + tokenFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.data").doesNotExist());

        UserEntity deleted = userRepository.findById(user.getUserId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    void deletedUserShouldBeForbiddenForMeAndLogin() throws Exception {
        UserEntity user = saveActiveUser("deleted_guard", "deleted.guard@example.com");
        String token = tokenFor(user);

        mockMvc.perform(delete(DELETE_ME_PATH)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + token))
                .andExpect(status().isOk());

        mockMvc.perform(get(DELETE_ME_PATH)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());

        mockMvc.perform(post(LOGIN_PATH)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "deleted_guard",
                                  "password": "delete123"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("akun tidak aktif"));
    }

    private String tokenFor(UserEntity user) {
        return jwtService.generateToken(user.getUserId(), user.getRole());
    }

    private UserEntity saveActiveUser(String username, String email) {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setDisplayName("Display " + username);
        user.setEmail(email);
        user.setRole(Role.PELAJAR);
        user.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
        return userRepository.saveAndFlush(user);
    }
}
