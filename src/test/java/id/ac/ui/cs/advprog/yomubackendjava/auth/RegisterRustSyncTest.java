package id.ac.ui.cs.advprog.yomubackendjava.auth;

import id.ac.ui.cs.advprog.yomubackendjava.integration.rust.RustEngineClient;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.domain.FailedSyncEventEntity;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.domain.SyncEventStatus;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.domain.SyncEventType;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.repo.FailedSyncEventRepository;
import id.ac.ui.cs.advprog.yomubackendjava.user.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
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
@Import(RegisterRustSyncTest.MockBeans.class)
class RegisterRustSyncTest {
    private static final String REGISTER_PATH = "/api/v1/auth/register";
    private static final String SUCCESS_JSON_PATH = "$.success";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RustEngineClient rustEngineClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FailedSyncEventRepository failedSyncEventRepository;

    @BeforeEach
    void setUp() {
        Mockito.reset(rustEngineClient);
        failedSyncEventRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registerShouldReturn200AndNoOutboxWhenRustReturns201() throws Exception {
        when(rustEngineClient.syncUser(any(UUID.class)))
                .thenReturn(new RustEngineClient.SyncResult(201, "created"));

        mockMvc.perform(post(REGISTER_PATH)
                        .contentType(APPLICATION_JSON)
                .content("""
                                {
                                  "username": "sync201",
                                  "display_name": "Sync 201",
                                  "password": "rahasia123",
                                  "email": "sync201@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath(SUCCESS_JSON_PATH).value(true));

        verify(rustEngineClient, times(1)).syncUser(any(UUID.class));
        assertThat(failedSyncEventRepository.count()).isZero();
    }

    @Test
    void registerShouldReturn200AndNoOutboxWhenRustReturns409() throws Exception {
        when(rustEngineClient.syncUser(any(UUID.class)))
                .thenReturn(new RustEngineClient.SyncResult(409, "conflict"));

        mockMvc.perform(post(REGISTER_PATH)
                        .contentType(APPLICATION_JSON)
                .content("""
                                {
                                  "username": "sync409",
                                  "display_name": "Sync 409",
                                  "password": "rahasia123",
                                  "phone_number": "+628111111111"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath(SUCCESS_JSON_PATH).value(true));

        verify(rustEngineClient, times(1)).syncUser(any(UUID.class));
        assertThat(failedSyncEventRepository.count()).isZero();
    }

    @Test
    void registerShouldReturn200AndCreateOutboxWhenRustThrowsException() throws Exception {
        when(rustEngineClient.syncUser(any(UUID.class)))
                .thenThrow(new RestClientException("timeout/down"));

        mockMvc.perform(post(REGISTER_PATH)
                        .contentType(APPLICATION_JSON)
                .content("""
                                {
                                  "username": "synctimeout",
                                  "display_name": "Sync Timeout",
                                  "password": "rahasia123",
                                  "email": "synctimeout@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath(SUCCESS_JSON_PATH).value(true));

        verify(rustEngineClient, times(2)).syncUser(any(UUID.class));

        List<FailedSyncEventEntity> events = failedSyncEventRepository.findAll();
        assertThat(events).hasSize(1);
        FailedSyncEventEntity event = events.get(0);
        assertThat(event.getEventType()).isEqualTo(SyncEventType.USER_SYNC);
        assertThat(event.getStatus()).isEqualTo(SyncEventStatus.FAILED);
        assertThat(event.getPayloadJson()).contains("user_id");
    }

    @Test
    void registerShouldRetryRustSyncBeforeCreatingOutbox() throws Exception {
        when(rustEngineClient.syncUser(any(UUID.class)))
                .thenThrow(new RestClientException("timeout/down"))
                .thenReturn(new RustEngineClient.SyncResult(201, "created"));

        mockMvc.perform(post(REGISTER_PATH)
                        .contentType(APPLICATION_JSON)
                .content("""
                                {
                                  "username": "syncretry",
                                  "display_name": "Sync Retry",
                                  "password": "rahasia123",
                                  "email": "syncretry@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath(SUCCESS_JSON_PATH).value(true));

        verify(rustEngineClient, times(2)).syncUser(any(UUID.class));
        assertThat(failedSyncEventRepository.count()).isZero();
    }

    @TestConfiguration
    static class MockBeans {
        @Bean
        @Primary
        RustEngineClient rustEngineClient() {
            return Mockito.mock(RustEngineClient.class);
        }
    }
}
