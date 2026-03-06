package id.ac.ui.cs.advprog.yomubackendjava.repo;

import id.ac.ui.cs.advprog.yomubackendjava.outbox.domain.FailedSyncEventEntity;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.domain.SyncEventStatus;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.domain.SyncEventType;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.repo.FailedSyncEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class FailedSyncEventRepositoryTest {
    @Autowired
    private FailedSyncEventRepository failedSyncEventRepository;

    @Test
    void insertEventSuccess() {
        FailedSyncEventEntity event = buildEvent(
                SyncEventStatus.PENDING,
                "{\"user_id\":\"u-1\"}",
                Instant.now().minusSeconds(30)
        );

        FailedSyncEventEntity saved = failedSyncEventRepository.saveAndFlush(event);

        assertThat(saved.getEventId()).isNotNull();
        assertThat(saved.getRetryCount()).isZero();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void findByStatusShouldReturnOrderedList() {
        failedSyncEventRepository.saveAndFlush(buildEvent(
                SyncEventStatus.FAILED,
                "{\"seq\":1}",
                Instant.parse("2026-01-01T00:00:01Z")
        ));
        failedSyncEventRepository.saveAndFlush(buildEvent(
                SyncEventStatus.PENDING,
                "{\"seq\":2}",
                Instant.parse("2026-01-01T00:00:02Z")
        ));
        failedSyncEventRepository.saveAndFlush(buildEvent(
                SyncEventStatus.DONE,
                "{\"seq\":3}",
                Instant.parse("2026-01-01T00:00:03Z")
        ));

        List<FailedSyncEventEntity> result = failedSyncEventRepository.findTop100ByStatusInOrderByCreatedAtAsc(
                List.of(SyncEventStatus.PENDING, SyncEventStatus.FAILED)
        );

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPayloadJson()).isEqualTo("{\"seq\":1}");
        assertThat(result.get(1).getPayloadJson()).isEqualTo("{\"seq\":2}");
    }

    private FailedSyncEventEntity buildEvent(SyncEventStatus status, String payloadJson, Instant createdAt) {
        FailedSyncEventEntity event = new FailedSyncEventEntity();
        event.setEventType(SyncEventType.USER_SYNC);
        event.setPayloadJson(payloadJson);
        event.setStatus(status);
        event.setRetryCount(0);
        event.setCreatedAt(createdAt);
        return event;
    }
}
