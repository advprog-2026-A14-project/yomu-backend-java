package id.ac.ui.cs.advprog.yomubackendjava.integration.rust;

import java.util.UUID;

public interface RustEngineClient {
    SyncResult syncUser(UUID userId);

    record SyncResult(int statusCode, String responseBody) {
    }
}
