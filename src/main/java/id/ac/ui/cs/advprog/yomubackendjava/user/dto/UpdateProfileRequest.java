package id.ac.ui.cs.advprog.yomubackendjava.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "hanya boleh berisi huruf, angka, titik, underscore, dan dash")
    private String username;

    @JsonProperty("display_name")
    @Size(max = 100)
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
