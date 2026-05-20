package id.ac.ui.cs.advprog.yomubackendjava.security;

import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.Role;
import id.ac.ui.cs.advprog.yomubackendjava.YomuBackendJavaApplication;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.UserEntity;
import id.ac.ui.cs.advprog.yomubackendjava.user.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {YomuBackendJavaApplication.class, SecurityIntegrationTest.DummySecureController.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "jwt.secret=01234567890123456789012345678901",
        "jwt.ttl-seconds=3600"
})
class SecurityIntegrationTest {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String SECURE_PING_PATH = "/api/v1/secure/ping";
    private static final String SUCCESS_JSON_PATH = "$.success";
    private static final String MESSAGE_JSON_PATH = "$.message";

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
    void requestWithoutTokenShouldReturnWrapped401() throws Exception {
        mockMvc.perform(get(SECURE_PING_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(SUCCESS_JSON_PATH).value(false))
                .andExpect(jsonPath(MESSAGE_JSON_PATH).isNotEmpty());
    }

    @Test
    void requestWithInvalidTokenShouldReturnWrapped401() throws Exception {
        mockMvc.perform(get(SECURE_PING_PATH)
                        .header(AUTHORIZATION_HEADER, BEARER_PREFIX + "invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(SUCCESS_JSON_PATH).value(false))
                .andExpect(jsonPath(MESSAGE_JSON_PATH).isNotEmpty());
    }

    @Test
    void requestWithValidTokenShouldReturn200() throws Exception {
        String token = jwtService.generateToken(UUID.randomUUID(), Role.PELAJAR);

        mockMvc.perform(get(SECURE_PING_PATH)
                        .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath(SUCCESS_JSON_PATH).value(true))
                .andExpect(jsonPath(MESSAGE_JSON_PATH).isNotEmpty());
    }

    @Test
    void tokenForDeletedUserShouldReturn403BeforeController() throws Exception {
        UserEntity user = new UserEntity();
        user.setUsername("security_deleted");
        user.setDisplayName("Security Deleted");
        user.setEmail("security.deleted@example.com");
        user.setRole(Role.PELAJAR);
        user.setPasswordHash("hash");
        user.setDeletedAt(Instant.now());
        UserEntity saved = userRepository.saveAndFlush(user);
        String token = jwtService.generateToken(saved.getUserId(), saved.getRole());

        mockMvc.perform(get(SECURE_PING_PATH)
                        .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(SUCCESS_JSON_PATH).value(false));
    }

    @Test
    void corsPreflightFromAllowedOriginShouldReturnCorsHeaders() throws Exception {
        mockMvc.perform(options(SECURE_PING_PATH)
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void corsPreflightFromUnknownOriginShouldBeRejected() throws Exception {
        mockMvc.perform(options(SECURE_PING_PATH)
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void malformedAuthorizationHeaderShouldReturn401() throws Exception {
        mockMvc.perform(get(SECURE_PING_PATH)
                        .header(AUTHORIZATION_HEADER, "Basic abc"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(SUCCESS_JSON_PATH).value(false));
    }

    @Test
    void pelajarTokenShouldGet403OnAdminEndpoint() throws Exception {
        String token = jwtService.generateToken(UUID.randomUUID(), Role.PELAJAR);

        mockMvc.perform(get("/api/v1/admin/ping")
                        .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(SUCCESS_JSON_PATH).value(false))
                .andExpect(jsonPath(MESSAGE_JSON_PATH).isNotEmpty());
    }

    @RestController
    @RequestMapping("/api/v1")
    static class DummySecureController {
        @GetMapping("/secure/ping")
        ApiResponse<Void> me() {
            return ApiResponse.success("me ok");
        }

        @GetMapping("/admin/ping")
        ApiResponse<Void> adminPing() {
            return ApiResponse.success("admin ok");
        }
    }
}
