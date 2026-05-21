package id.ac.ui.cs.advprog.yomubackendjava.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class GoogleLoginRequest {
    @JsonProperty("id_token")
    @NotBlank
    @Size(max = 4096)
    private String idToken;

    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "hanya boleh berisi huruf, angka, titik, underscore, dan dash")
    private String username;

    @JsonProperty("display_name")
    @Size(max = 100)
    private String displayName;

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

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
