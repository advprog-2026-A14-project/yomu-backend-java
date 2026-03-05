package id.ac.ui.cs.advprog.yomubackendjava.security;

import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.Role;
import id.ac.ui.cs.advprog.yomubackendjava.YomuBackendJavaApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void requestWithoutTokenShouldReturnWrapped401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void requestWithInvalidTokenShouldReturnWrapped401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header(AUTHORIZATION_HEADER, BEARER_PREFIX + "invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void requestWithValidTokenShouldReturn200() throws Exception {
        String token = jwtService.generateToken(UUID.randomUUID(), Role.PELAJAR);

        mockMvc.perform(get("/api/v1/users/me")
                        .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void pelajarTokenShouldGet403OnAdminEndpoint() throws Exception {
        String token = jwtService.generateToken(UUID.randomUUID(), Role.PELAJAR);

        mockMvc.perform(get("/api/v1/admin/ping")
                        .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @RestController
    @RequestMapping("/api/v1")
    static class DummySecureController {
        @GetMapping("/users/me")
        ApiResponse<Void> me() {
            return ApiResponse.success("me ok");
        }

        @GetMapping("/admin/ping")
        ApiResponse<Void> adminPing() {
            return ApiResponse.success("admin ok");
        }
    }
}
