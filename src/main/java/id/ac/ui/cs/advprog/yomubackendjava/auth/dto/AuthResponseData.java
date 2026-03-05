package id.ac.ui.cs.advprog.yomubackendjava.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthResponseData {
    @JsonProperty("access_token")
    private String accessToken;
    private UserDto user;

    public AuthResponseData(String accessToken, UserDto user) {
        this.accessToken = accessToken;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public UserDto getUser() {
        return user;
    }
}
