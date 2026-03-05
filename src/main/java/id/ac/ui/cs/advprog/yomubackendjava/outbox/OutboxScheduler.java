package id.ac.ui.cs.advprog.yomubackendjava.outbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "outbox.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxScheduler {
    private final OutboxRetryService outboxRetryService;
    private final int maxRetry;

    public OutboxScheduler(
            OutboxRetryService outboxRetryService,
            @Value("${outbox.retry.max-attempts:5}") int maxRetry
    ) {
        this.outboxRetryService = outboxRetryService;
        this.maxRetry = maxRetry;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void retryFailedSyncEvents() {
        outboxRetryService.retryFailedFromScheduler(maxRetry);
    }
}
