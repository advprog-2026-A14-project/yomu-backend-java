package id.ac.ui.cs.advprog.yomubackendjava.auth;

import id.ac.ui.cs.advprog.yomubackendjava.auth.google.GoogleIdTokenVerifier;
import id.ac.ui.cs.advprog.yomubackendjava.auth.google.GoogleProfile;
import id.ac.ui.cs.advprog.yomubackendjava.integration.rust.RustEngineClient;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.domain.FailedSyncEventEntity;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.domain.SyncEventStatus;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.domain.SyncEventType;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.repo.FailedSyncEventRepository;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.Role;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.UserEntity;
import id.ac.ui.cs.advprog.yomubackendjava.user.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(GoogleLoginTest.MockBeans.class)
class GoogleLoginTest {
    private static final String GOOGLE_LOGIN_PATH = "/api/v1/auth/google";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    @Autowired
    private RustEngineClient rustEngineClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FailedSyncEventRepository failedSyncEventRepository;

    @BeforeEach
    void setUp() {
        Mockito.reset(googleIdTokenVerifier, rustEngineClient);
        failedSyncEventRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void invalidTokenShouldReturn400WrapperError() throws Exception {
        when(googleIdTokenVerifier.verify("invalid-token")).thenThrow(new IllegalArgumentException("invalid"));

        mockMvc.perform(post(GOOGLE_LOGIN_PATH)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "id_token": "invalid-token"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void firstLoginShouldCreateUserAndReturnIsNewUserTrue() throws Exception {
        when(googleIdTokenVerifier.verify("google-new"))
                .thenReturn(new GoogleProfile("gsub-new", "new.google@example.com", "Google Name"));
        when(rustEngineClient.syncUser(any(UUID.class)))
                .thenReturn(new RustEngineClient.SyncResult(201, "created"));

        mockMvc.perform(post(GOOGLE_LOGIN_PATH)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "id_token": "google-new",
                                  "display_name": "Display Google"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.is_new_user").value(true))
                .andExpect(jsonPath("$.data.access_token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.role").value("PELAJAR"));

        UserEntity user = userRepository.findByGoogleSubAndDeletedAtIsNull("gsub-new").orElseThrow();
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.getDisplayName()).isEqualTo("Display Google");
        assertThat(user.getRole()).isEqualTo(Role.PELAJAR);
    }

    @Test
    void secondLoginShouldReturnIsNewUserFalse() throws Exception {
        UserEntity existing = new UserEntity();
        existing.setUsername("google_existing");
        existing.setDisplayName("Google Existing");
        existing.setEmail("google.existing@example.com");
        existing.setRole(Role.PELAJAR);
        existing.setPasswordHash(null);
        existing.setGoogleSub("gsub-existing");
        userRepository.saveAndFlush(existing);

        when(googleIdTokenVerifier.verify("google-existing"))
                .thenReturn(new GoogleProfile("gsub-existing", "google.existing@example.com", "Existing"));

        mockMvc.perform(post(GOOGLE_LOGIN_PATH)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "id_token": "google-existing"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.is_new_user").value(false))
                .andExpect(jsonPath("$.data.user.user_id").value(existing.getUserId().toString()));

        assertThat(userRepository.count()).isEqualTo(1L);
    }

    @Test
    void firstLoginRustExceptionShouldStill200AndCreateOutbox() throws Exception {
        when(googleIdTokenVerifier.verify("google-rust-fail"))
                .thenReturn(new GoogleProfile("gsub-rust-fail", "rust.fail@example.com", "Rust Fail"));
        when(rustEngineClient.syncUser(any(UUID.class)))
                .thenThrow(new RestClientException("timeout/down"));

        mockMvc.perform(post(GOOGLE_LOGIN_PATH)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "id_token": "google-rust-fail",
                                  "username": "google_rust_fail"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.is_new_user").value(true));

        List<FailedSyncEventEntity> events = failedSyncEventRepository.findAll();
        assertThat(events).hasSize(1);
        FailedSyncEventEntity event = events.get(0);
        assertThat(event.getEventType()).isEqualTo(SyncEventType.USER_SYNC);
        assertThat(event.getStatus()).isEqualTo(SyncEventStatus.FAILED);
        assertThat(event.getPayloadJson()).contains("user_id");
    }

    @Test
    void usernameConflictOnFirstLoginShouldReturn409() throws Exception {
        UserEntity existing = new UserEntity();
        existing.setUsername("taken_username");
        existing.setDisplayName("Taken Username");
        existing.setEmail("taken@example.com");
        existing.setRole(Role.PELAJAR);
        existing.setPasswordHash("hash");
        userRepository.saveAndFlush(existing);

        when(googleIdTokenVerifier.verify("google-conflict"))
                .thenReturn(new GoogleProfile("gsub-conflict", "conflict@example.com", "Conflict"));

        mockMvc.perform(post(GOOGLE_LOGIN_PATH)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "id_token": "google-conflict",
                                  "username": "taken_username"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @TestConfiguration
    static class MockBeans {
        @Bean
        @Primary
        GoogleIdTokenVerifier googleIdTokenVerifier() {
            return Mockito.mock(GoogleIdTokenVerifier.class);
        }

        @Bean
        @Primary
        RustEngineClient rustEngineClient() {
            return Mockito.mock(RustEngineClient.class);
        }
    }
}
