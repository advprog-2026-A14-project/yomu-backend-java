package id.ac.ui.cs.advprog.yomubackendjava.integration.rust.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SyncUserRequest {
    @JsonProperty("user_id")
    private final String userId;

    public SyncUserRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
