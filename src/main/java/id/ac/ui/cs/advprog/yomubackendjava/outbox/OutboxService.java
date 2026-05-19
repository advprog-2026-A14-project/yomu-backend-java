package id.ac.ui.cs.advprog.yomubackendjava.outbox;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizSyncRequest;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.domain.FailedSyncEventEntity;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.domain.SyncEventStatus;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.domain.SyncEventType;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.repo.FailedSyncEventRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;
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
        event.setPayloadJson(buildUserSyncPayloadJson(userId));
        event.setStatus(SyncEventStatus.FAILED);
        event.setRetryCount(0);
        event.setLastError(lastError);
        failedSyncEventRepository.save(event);
    }

    public void recordQuizSyncFailure(QuizSyncRequest request, String lastError) {
        FailedSyncEventEntity event = new FailedSyncEventEntity();
        event.setEventType(SyncEventType.QUIZ_SYNC);
        event.setPayloadJson(buildQuizSyncPayloadJson(request));
        event.setStatus(SyncEventStatus.FAILED);
        event.setRetryCount(0);
        event.setLastError(lastError);
        failedSyncEventRepository.save(event);
    }

    private String buildUserSyncPayloadJson(UUID userId) {
        return "{\"user_id\":\"" + userId + "\"}";
    }

    private String buildQuizSyncPayloadJson(QuizSyncRequest request) {
        return String.format(
                Locale.US,
                "{\"user_id\":\"%s\",\"article_id\":\"%s\",\"score\":%s,\"accuracy\":%s}",
                request.getUserId(),
                escapeJson(request.getArticleId()),
                Double.toString(request.getScore()),
                Double.toString(request.getAccuracy())
        );
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
