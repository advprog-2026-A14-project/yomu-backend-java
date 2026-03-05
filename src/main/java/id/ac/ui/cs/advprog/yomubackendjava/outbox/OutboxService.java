package id.ac.ui.cs.advprog.yomubackendjava.outbox;

import id.ac.ui.cs.advprog.yomubackendjava.outbox.domain.FailedSyncEventEntity;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.domain.SyncEventStatus;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.domain.SyncEventType;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.repo.FailedSyncEventRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OutboxService {
    private final FailedSyncEventRepository failedSyncEventRepository;

    public OutboxService(FailedSyncEventRepository failedSyncEventRepository) {
        this.failedSyncEventRepository = failedSyncEventRepository;
    }

    public void recordUserSyncFailure(UUID userId, String lastError) {
        FailedSyncEventEntity event = new FailedSyncEventEntity();
        event.setEventType(SyncEventType.USER_SYNC);
        event.setPayloadJson("{\"user_id\":\"" + userId + "\"}");
        event.setStatus(SyncEventStatus.FAILED);
        event.setRetryCount(0);
        event.setLastError(lastError);
        failedSyncEventRepository.save(event);
    }
}
