package id.ac.ui.cs.advprog.yomubackendjava.auth;

import id.ac.ui.cs.advprog.yomubackendjava.integration.rust.RustEngineClient;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Service
public class AuthUserSyncService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthUserSyncService.class);
    private static final int RUST_SYNC_CREATED_STATUS = 201;
    private static final int RUST_SYNC_CONFLICT_STATUS = 409;

    private final RustEngineClient rustEngineClient;
    private final OutboxService outboxService;
    private final int maxAttempts;

    public AuthUserSyncService(
            RustEngineClient rustEngineClient,
            OutboxService outboxService,
            @Value("${auth.user-sync.retry.max-attempts:2}") int maxAttempts
    ) {
        this.rustEngineClient = rustEngineClient;
        this.outboxService = outboxService;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public void syncNewUser(UUID userId) {
        SyncAttemptResult result = attemptSync(userId);
        if (result.succeeded()) {
            return;
        }

        LOGGER.error("Rust sync gagal untuk user_id={} {}", userId, result.errorMessage());
        outboxService.recordUserSyncFailure(userId, result.errorMessage());
    }

    public SyncAttemptResult retryUserSync(UUID userId) {
        return attemptSync(userId);
    }

    private SyncAttemptResult attemptSync(UUID userId) {
        String lastError = "rust sync gagal";
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                RustEngineClient.SyncResult syncResult = rustEngineClient.syncUser(userId);
                if (isSuccessful(syncResult.statusCode())) {
                    return SyncAttemptResult.ok();
                }

                lastError = "status=" + syncResult.statusCode() + " body=" + syncResult.responseBody();
                if (!isRetryableStatus(syncResult.statusCode()) || attempt == maxAttempts) {
                    return SyncAttemptResult.failure(lastError);
                }
                LOGGER.warn(
                        "Rust sync retry dijadwalkan untuk user_id={} attempt={}/{} status={}",
                        userId,
                        attempt + 1,
                        maxAttempts,
                        syncResult.statusCode()
                );
            } catch (RestClientException ex) {
                lastError = messageOrDefault(ex.getMessage(), "rust sync request gagal");
                if (attempt == maxAttempts) {
                    return SyncAttemptResult.failure(lastError);
                }
                LOGGER.warn(
                        "Rust sync retry dijadwalkan untuk user_id={} attempt={}/{} karena exception={}",
                        userId,
                        attempt + 1,
                        maxAttempts,
                        ex.getClass().getSimpleName()
                );
            } catch (RuntimeException ex) {
                lastError = messageOrDefault(ex.getMessage(), "rust sync runtime gagal");
                return SyncAttemptResult.failure(lastError);
            }
        }
        return SyncAttemptResult.failure(lastError);
    }

    private boolean isSuccessful(int statusCode) {
        return statusCode == RUST_SYNC_CREATED_STATUS || statusCode == RUST_SYNC_CONFLICT_STATUS;
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 408 || statusCode == 429 || statusCode >= 500;
    }

    private String messageOrDefault(String message, String defaultMessage) {
        return message == null || message.isBlank() ? defaultMessage : message;
    }

    public record SyncAttemptResult(boolean succeeded, String errorMessage) {
        public static SyncAttemptResult ok() {
            return new SyncAttemptResult(true, null);
        }

        public static SyncAttemptResult failure(String errorMessage) {
            return new SyncAttemptResult(false, errorMessage);
        }
    }
}
