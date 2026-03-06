package id.ac.ui.cs.advprog.yomubackendjava.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateProfileRequest {
    private String username;

    @JsonProperty("display_name")
    private String displayName;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
