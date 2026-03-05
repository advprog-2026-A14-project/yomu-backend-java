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

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserBatchTest {
    private static final String BATCH_PATH = "/api/v1/users/batch";

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
    void batchAllFoundShouldReturn200() throws Exception {
        UserEntity user1 = saveUser("batch_u1", "Batch User 1");
        UserEntity user2 = saveUser("batch_u2", "Batch User 2");
        String ids = user1.getUserId() + "," + user2.getUserId();

        mockMvc.perform(get(BATCH_PATH)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, bearerPelajarToken())
                        .queryParam("ids", ids))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.users.length()").value(2))
                .andExpect(jsonPath("$.data.not_found_ids.length()").value(0));
    }

    @Test
    void batchPartialFoundShouldReturnNotFoundIds() throws Exception {
        UserEntity user = saveUser("batch_partial", "Batch Partial");
        String missingId = UUID.randomUUID().toString();
        String ids = user.getUserId() + "," + missingId;

        mockMvc.perform(get(BATCH_PATH)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, bearerPelajarToken())
                        .queryParam("ids", ids))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.users.length()").value(1))
                .andExpect(jsonPath("$.data.not_found_ids.length()").value(1))
                .andExpect(jsonPath("$.data.not_found_ids[0]").value(missingId));
    }

    @Test
    void batchMoreThan100IdsShouldReturn400() throws Exception {
        String ids = IntStream.range(0, 101)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.joining(","));

        mockMvc.perform(get(BATCH_PATH)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, bearerPelajarToken())
                        .queryParam("ids", ids))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void batchInvalidUuidShouldReturn400() throws Exception {
        mockMvc.perform(get(BATCH_PATH)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, bearerPelajarToken())
                        .queryParam("ids", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    private String bearerPelajarToken() {
        return JwtAuthFilter.BEARER_PREFIX + jwtService.generateToken(UUID.randomUUID(), Role.PELAJAR);
    }

    private UserEntity saveUser(String username, String displayName) {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setRole(Role.PELAJAR);
        user.setPasswordHash("hash");
        return userRepository.saveAndFlush(user);
    }
}
