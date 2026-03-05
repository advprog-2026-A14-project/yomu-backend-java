package id.ac.ui.cs.advprog.yomubackendjava.admin;

import id.ac.ui.cs.advprog.yomubackendjava.integration.rust.RustEngineClient;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.domain.FailedSyncEventEntity;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.domain.SyncEventStatus;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.domain.SyncEventType;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.repo.FailedSyncEventRepository;
import id.ac.ui.cs.advprog.yomubackendjava.security.JwtAuthFilter;
import id.ac.ui.cs.advprog.yomubackendjava.security.JwtService;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.Role;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AdminOutboxTest.MockBeans.class)
class AdminOutboxTest {
    private static final String ADMIN_EVENTS_PATH = "/api/v1/admin/failed-sync-events";
    private static final String ADMIN_RETRY_PATH = "/api/v1/admin/failed-sync-events/retry";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private FailedSyncEventRepository failedSyncEventRepository;

    @Autowired
    private RustEngineClient rustEngineClient;

    @BeforeEach
    void setUp() {
        Mockito.reset(rustEngineClient);
        failedSyncEventRepository.deleteAll();
    }

    @Test
    void pelajarTokenShouldGet403() throws Exception {
        mockMvc.perform(get(ADMIN_EVENTS_PATH)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, bearerToken(Role.PELAJAR)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void adminTokenShouldGet200ForListEvents() throws Exception {
        failedSyncEventRepository.saveAndFlush(buildEvent(UUID.randomUUID(), SyncEventStatus.FAILED, 0));

        mockMvc.perform(get(ADMIN_EVENTS_PATH)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, bearerToken(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.events.length()").value(1));
    }

    @Test
    void retrySuccessShouldMarkDone() throws Exception {
        UUID userId = UUID.randomUUID();
        FailedSyncEventEntity event = failedSyncEventRepository.saveAndFlush(buildEvent(userId, SyncEventStatus.FAILED, 0));
        when(rustEngineClient.syncUser(userId)).thenReturn(new RustEngineClient.SyncResult(201, "created"));

        mockMvc.perform(post(ADMIN_RETRY_PATH)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, bearerToken(Role.ADMIN))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "event_ids": [%d]
                                }
                                """.formatted(event.getEventId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        FailedSyncEventEntity updated = failedSyncEventRepository.findById(event.getEventId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(SyncEventStatus.DONE);
    }

    @Test
    void retry409ShouldMarkDone() throws Exception {
        UUID userId = UUID.randomUUID();
        FailedSyncEventEntity event = failedSyncEventRepository.saveAndFlush(buildEvent(userId, SyncEventStatus.FAILED, 0));
        when(rustEngineClient.syncUser(userId)).thenReturn(new RustEngineClient.SyncResult(409, "conflict"));

        mockMvc.perform(post(ADMIN_RETRY_PATH)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, bearerToken(Role.ADMIN))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "event_ids": [%d]
                                }
                                """.formatted(event.getEventId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        FailedSyncEventEntity updated = failedSyncEventRepository.findById(event.getEventId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(SyncEventStatus.DONE);
    }

    @Test
    void retryFailShouldIncrementRetryCount() throws Exception {
        UUID userId = UUID.randomUUID();
        FailedSyncEventEntity event = failedSyncEventRepository.saveAndFlush(buildEvent(userId, SyncEventStatus.FAILED, 1));
        when(rustEngineClient.syncUser(userId)).thenReturn(new RustEngineClient.SyncResult(500, "boom"));

        mockMvc.perform(post(ADMIN_RETRY_PATH)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, bearerToken(Role.ADMIN))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "event_ids": [%d]
                                }
                                """.formatted(event.getEventId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        FailedSyncEventEntity updated = failedSyncEventRepository.findById(event.getEventId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(SyncEventStatus.FAILED);
        assertThat(updated.getRetryCount()).isEqualTo(2);
    }

    private String bearerToken(Role role) {
        return JwtAuthFilter.BEARER_PREFIX + jwtService.generateToken(UUID.randomUUID(), role);
    }

    private FailedSyncEventEntity buildEvent(UUID userId, SyncEventStatus status, int retryCount) {
        FailedSyncEventEntity event = new FailedSyncEventEntity();
        event.setEventType(SyncEventType.USER_SYNC);
        event.setPayloadJson("{\"user_id\":\"" + userId + "\"}");
        event.setStatus(status);
        event.setRetryCount(retryCount);
        return event;
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
