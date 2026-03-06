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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MeEndpointTest {
    private static final String ME_PATH = "/api/v1/users/me";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void meWithoutTokenShouldReturn401Wrapper() throws Exception {
        mockMvc.perform(get(ME_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void meWithValidTokenShouldReturn200AndMatchingUserId() throws Exception {
        UserEntity user = new UserEntity();
        user.setUsername("me_user");
        user.setDisplayName("Me User");
        user.setEmail("me.user@example.com");
        user.setRole(Role.PELAJAR);
        user.setPasswordHash("hash");
        UserEntity saved = userRepository.saveAndFlush(user);

        String token = jwtService.generateToken(saved.getUserId(), saved.getRole());

        mockMvc.perform(get(ME_PATH)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user_id").value(saved.getUserId().toString()));
    }

    @Test
    void meForDeletedUserShouldReturn403Wrapper() throws Exception {
        UserEntity user = new UserEntity();
        user.setUsername("me_deleted");
        user.setDisplayName("Me Deleted");
        user.setPhoneNumber("+6281212121212");
        user.setRole(Role.PELAJAR);
        user.setPasswordHash("hash");
        user.setDeletedAt(Instant.now());
        UserEntity saved = userRepository.saveAndFlush(user);

        String token = jwtService.generateToken(saved.getUserId(), saved.getRole());

        mockMvc.perform(get(ME_PATH)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
