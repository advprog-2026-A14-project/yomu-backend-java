package id.ac.ui.cs.advprog.yomubackendjava.outbox;

import com.fasterxml.jackson.annotation.JsonProperty;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.BadRequestException;
import id.ac.ui.cs.advprog.yomubackendjava.integration.rust.RustEngineClient;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.domain.FailedSyncEventEntity;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.domain.SyncEventStatus;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.domain.SyncEventType;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.repo.FailedSyncEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OutboxRetryService {
    private static final int RUST_SYNC_CREATED_STATUS = 201;
    private static final int RUST_SYNC_CONFLICT_STATUS = 409;
    private static final String RETRY_REQUEST_REQUIRED_MESSAGE = "event_ids atau retry_all wajib diisi";
    private static final String USER_ID_PAYLOAD_INVALID_MESSAGE = "payload user_id tidak valid";
    private static final Pattern USER_ID_PATTERN = Pattern.compile("\"user_id\"\\s*:\\s*\"([^\"]+)\"");

    private final FailedSyncEventRepository failedSyncEventRepository;
    private final RustEngineClient rustEngineClient;

    public OutboxRetryService(
            FailedSyncEventRepository failedSyncEventRepository,
            RustEngineClient rustEngineClient
    ) {
        this.failedSyncEventRepository = failedSyncEventRepository;
        this.rustEngineClient = rustEngineClient;
    }

    public FailedSyncEventsData listFailedSyncEvents() {
        List<FailedSyncEventEntity> events = failedSyncEventRepository.findTop100ByStatusInOrderByCreatedAtAsc(
                List.of(SyncEventStatus.FAILED, SyncEventStatus.PENDING)
        );
        List<FailedSyncEventView> mapped = new ArrayList<>();
        for (FailedSyncEventEntity event : events) {
            mapped.add(toView(event));
        }
        return new FailedSyncEventsData(mapped);
    }

    public RetrySummary retryEvents(Collection<Long> eventIds, boolean retryAll) {
        List<FailedSyncEventEntity> events = resolveEvents(eventIds, retryAll);
        int doneCount = 0;
        int failedCount = 0;
        for (FailedSyncEventEntity event : events) {
            SyncEventStatus result = retryOneEvent(event);
            if (result == SyncEventStatus.DONE) {
                doneCount++;
            } else if (result == SyncEventStatus.FAILED) {
                failedCount++;
            }
        }
        return new RetrySummary(events.size(), doneCount, failedCount);
    }

    public int retryFailedFromScheduler(int maxRetry) {
        List<FailedSyncEventEntity> events = failedSyncEventRepository.findTop100ByStatusInOrderByCreatedAtAsc(
                List.of(SyncEventStatus.FAILED, SyncEventStatus.PENDING)
        );
        int retriedCount = 0;
        for (FailedSyncEventEntity event : events) {
            if (event.getRetryCount() < maxRetry) {
                retryOneEvent(event);
                retriedCount++;
            }
        }
        return retriedCount;
    }

    private List<FailedSyncEventEntity> resolveEvents(Collection<Long> eventIds, boolean retryAll) {
        if (retryAll) {
            return failedSyncEventRepository.findTop100ByStatusInOrderByCreatedAtAsc(
                    List.of(SyncEventStatus.FAILED, SyncEventStatus.PENDING)
            );
        }

        if (eventIds == null || eventIds.isEmpty()) {
            throw new BadRequestException(RETRY_REQUEST_REQUIRED_MESSAGE);
        }
        return failedSyncEventRepository.findAllById(eventIds);
    }

    private SyncEventStatus retryOneEvent(FailedSyncEventEntity event) {
        if (event.getEventType() != SyncEventType.USER_SYNC) {
            markFailed(event, "event_type tidak didukung");
            return SyncEventStatus.FAILED;
        }

        try {
            UUID userId = extractUserId(event.getPayloadJson());
            RustEngineClient.SyncResult result = rustEngineClient.syncUser(userId);
            if (result.statusCode() == RUST_SYNC_CREATED_STATUS || result.statusCode() == RUST_SYNC_CONFLICT_STATUS) {
                markDone(event);
                return SyncEventStatus.DONE;
            }

            markFailed(event, "status=" + result.statusCode() + " body=" + result.responseBody());
            return SyncEventStatus.FAILED;
        } catch (RuntimeException ex) {
            markFailed(event, ex.getMessage() == null ? "retry gagal" : ex.getMessage());
            return SyncEventStatus.FAILED;
        }
    }

    private UUID extractUserId(String payloadJson) {
        try {
            Matcher matcher = USER_ID_PATTERN.matcher(payloadJson == null ? "" : payloadJson);
            if (!matcher.find()) {
                throw new BadRequestException(USER_ID_PAYLOAD_INVALID_MESSAGE);
            }
            return UUID.fromString(matcher.group(1));
        } catch (RuntimeException ex) {
            throw new BadRequestException(USER_ID_PAYLOAD_INVALID_MESSAGE);
        }
    }

    private void markDone(FailedSyncEventEntity event) {
        event.setStatus(SyncEventStatus.DONE);
        event.setLastError(null);
        failedSyncEventRepository.save(event);
    }

    private void markFailed(FailedSyncEventEntity event, String errorMessage) {
        event.setStatus(SyncEventStatus.FAILED);
        event.setRetryCount(event.getRetryCount() + 1);
        event.setLastError(errorMessage);
        failedSyncEventRepository.save(event);
    }

    private FailedSyncEventView toView(FailedSyncEventEntity event) {
        return new FailedSyncEventView(
                event.getEventId(),
                event.getEventType().name(),
                event.getPayloadJson(),
                event.getStatus().name(),
                event.getRetryCount(),
                event.getLastError(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    public record FailedSyncEventsData(List<FailedSyncEventView> events) {
    }

    public record FailedSyncEventView(
            @JsonProperty("event_id") Long eventId,
            @JsonProperty("event_type") String eventType,
            @JsonProperty("payload_json") String payloadJson,
            String status,
            @JsonProperty("retry_count") int retryCount,
            @JsonProperty("last_error") String lastError,
            @JsonProperty("created_at") Instant createdAt,
            @JsonProperty("updated_at") Instant updatedAt
    ) {
    }

    public record RetrySummary(
            @JsonProperty("processed_count") int processedCount,
            @JsonProperty("done_count") int doneCount,
            @JsonProperty("failed_count") int failedCount
    ) {
    }
}
