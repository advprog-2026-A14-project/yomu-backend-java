package id.ac.ui.cs.advprog.yomubackendjava.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.OutboxRetryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/failed-sync-events")
public class AdminOutboxController {
    private static final String LIST_SUCCESS_MESSAGE = "Daftar failed sync events berhasil diambil";
    private static final String RETRY_SUCCESS_MESSAGE = "Retry failed sync events selesai";

    private final OutboxRetryService outboxRetryService;

    public AdminOutboxController(OutboxRetryService outboxRetryService) {
        this.outboxRetryService = outboxRetryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<OutboxRetryService.FailedSyncEventsData>> listFailedSyncEvents() {
        return ResponseEntity.ok(ApiResponse.success(
                LIST_SUCCESS_MESSAGE,
                outboxRetryService.listFailedSyncEvents()
        ));
    }

    @PostMapping("/retry")
    public ResponseEntity<ApiResponse<OutboxRetryService.RetrySummary>> retryFailedSyncEvents(
            @RequestBody RetryFailedSyncRequest request
    ) {
        boolean retryAll = Boolean.TRUE.equals(request.getRetryAll());
        OutboxRetryService.RetrySummary summary = outboxRetryService.retryEvents(request.getEventIds(), retryAll);
        return ResponseEntity.ok(ApiResponse.success(RETRY_SUCCESS_MESSAGE, summary));
    }

    public static class RetryFailedSyncRequest {
        @JsonProperty("event_ids")
        private List<Long> eventIds;
        @JsonProperty("retry_all")
        private Boolean retryAll;

        public List<Long> getEventIds() {
            return eventIds;
        }

        public void setEventIds(List<Long> eventIds) {
            this.eventIds = eventIds;
        }

        public Boolean getRetryAll() {
            return retryAll;
        }

        public void setRetryAll(Boolean retryAll) {
            this.retryAll = retryAll;
        }
    }
}
