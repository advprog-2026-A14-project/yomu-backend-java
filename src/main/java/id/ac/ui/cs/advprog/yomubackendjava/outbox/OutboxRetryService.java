package id.ac.ui.cs.advprog.yomubackendjava.outbox;

import com.fasterxml.jackson.annotation.JsonProperty;
import id.ac.ui.cs.advprog.yomubackendjava.auth.AuthUserSyncService;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizSyncRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.integration.QuizSyncClient;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.BadRequestException;
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
import java.util.function.Predicate;

@Service
public class OutboxRetryService {
    private static final String RETRY_REQUEST_REQUIRED_MESSAGE = "event_ids atau retry_all wajib diisi";
    private static final String USER_ID_PAYLOAD_INVALID_MESSAGE = "payload user_id tidak valid";
    private static final String QUIZ_SYNC_PAYLOAD_INVALID_MESSAGE = "payload quiz sync tidak valid";
    private static final Pattern USER_ID_PATTERN = Pattern.compile("\"user_id\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ARTICLE_ID_PATTERN = Pattern.compile("\"article_id\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SCORE_PATTERN = Pattern.compile("\"score\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
    private static final Pattern ACCURACY_PATTERN = Pattern.compile("\"accuracy\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
    private static final List<SyncEventStatus> RETRYABLE_STATUSES = List.of(SyncEventStatus.FAILED, SyncEventStatus.PENDING);

    private final FailedSyncEventRepository failedSyncEventRepository;
    private final AuthUserSyncService authUserSyncService;
    private final QuizSyncClient quizSyncClient;

    public OutboxRetryService(
            FailedSyncEventRepository failedSyncEventRepository,
            AuthUserSyncService authUserSyncService,
            QuizSyncClient quizSyncClient
    ) {
        this.failedSyncEventRepository = failedSyncEventRepository;
        this.authUserSyncService = authUserSyncService;
        this.quizSyncClient = quizSyncClient;
    }

    public FailedSyncEventsData listFailedSyncEvents() {
        List<FailedSyncEventEntity> events = failedSyncEventRepository.findTop100ByStatusInOrderByCreatedAtAsc(RETRYABLE_STATUSES);
        List<FailedSyncEventView> mapped = new ArrayList<>();
        for (FailedSyncEventEntity event : events) {
            mapped.add(toView(event));
        }
        return new FailedSyncEventsData(mapped);
    }

    public RetrySummary retryEvents(Collection<Long> eventIds, boolean retryAll) {
        return retryEventsInternal(resolveEvents(eventIds, retryAll), event -> true);
    }

    public int retryFailedFromScheduler(int maxRetry) {
        RetrySummary summary = retryEventsInternal(
                failedSyncEventRepository.findTop100ByStatusInOrderByCreatedAtAsc(RETRYABLE_STATUSES),
                event -> event.getRetryCount() < maxRetry
        );
        return summary.processedCount();
    }

    private List<FailedSyncEventEntity> resolveEvents(Collection<Long> eventIds, boolean retryAll) {
        if (retryAll) {
            return failedSyncEventRepository.findTop100ByStatusInOrderByCreatedAtAsc(RETRYABLE_STATUSES);
        }

        if (eventIds == null || eventIds.isEmpty()) {
            throw new BadRequestException(RETRY_REQUEST_REQUIRED_MESSAGE);
        }
        return failedSyncEventRepository.findAllById(eventIds);
    }

    private RetrySummary retryEventsInternal(
            List<FailedSyncEventEntity> events,
            Predicate<FailedSyncEventEntity> predicate
    ) {
        int processedCount = 0;
        int doneCount = 0;
        int failedCount = 0;
        for (FailedSyncEventEntity event : events) {
            if (!predicate.test(event)) {
                continue;
            }
            processedCount++;
            SyncEventStatus result = retryOneEvent(event);
            if (result == SyncEventStatus.DONE) {
                doneCount++;
            } else if (result == SyncEventStatus.FAILED) {
                failedCount++;
            }
        }
        return new RetrySummary(processedCount, doneCount, failedCount);
    }

    private SyncEventStatus retryOneEvent(FailedSyncEventEntity event) {
        if (event.getEventType() == SyncEventType.USER_SYNC) {
            return retryUserSyncEvent(event);
        }
        if (event.getEventType() == SyncEventType.QUIZ_SYNC) {
            return retryQuizSyncEvent(event);
        }

        markFailed(event, "event_type tidak didukung");
        return SyncEventStatus.FAILED;
    }

    private SyncEventStatus retryUserSyncEvent(FailedSyncEventEntity event) {
        try {
            UUID userId = extractUserId(event.getPayloadJson());
            AuthUserSyncService.SyncAttemptResult result = authUserSyncService.retryUserSync(userId);
            if (result.succeeded()) {
                markDone(event);
                return SyncEventStatus.DONE;
            }

            markFailed(event, result.errorMessage());
            return SyncEventStatus.FAILED;
        } catch (RuntimeException ex) {
            markFailed(event, ex.getMessage() == null ? "retry gagal" : ex.getMessage());
            return SyncEventStatus.FAILED;
        }
    }

    private SyncEventStatus retryQuizSyncEvent(FailedSyncEventEntity event) {
        try {
            quizSyncClient.sync(extractQuizSyncRequest(event.getPayloadJson()));
            markDone(event);
            return SyncEventStatus.DONE;
        } catch (RuntimeException ex) {
            markFailed(event, ex.getMessage() == null ? "retry quiz sync gagal" : ex.getMessage());
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

    private QuizSyncRequest extractQuizSyncRequest(String payloadJson) {
        try {
            return new QuizSyncRequest(
                    extractUserId(payloadJson),
                    extractRequiredString(payloadJson, ARTICLE_ID_PATTERN),
                    extractRequiredDouble(payloadJson, SCORE_PATTERN),
                    extractRequiredDouble(payloadJson, ACCURACY_PATTERN)
            );
        } catch (RuntimeException ex) {
            throw new BadRequestException(QUIZ_SYNC_PAYLOAD_INVALID_MESSAGE);
        }
    }

    private String extractRequiredString(String payloadJson, Pattern pattern) {
        Matcher matcher = pattern.matcher(payloadJson == null ? "" : payloadJson);
        if (!matcher.find()) {
            throw new BadRequestException(QUIZ_SYNC_PAYLOAD_INVALID_MESSAGE);
        }
        return matcher.group(1);
    }

    private double extractRequiredDouble(String payloadJson, Pattern pattern) {
        Matcher matcher = pattern.matcher(payloadJson == null ? "" : payloadJson);
        if (!matcher.find()) {
            throw new BadRequestException(QUIZ_SYNC_PAYLOAD_INVALID_MESSAGE);
        }
        return Double.parseDouble(matcher.group(1));
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
