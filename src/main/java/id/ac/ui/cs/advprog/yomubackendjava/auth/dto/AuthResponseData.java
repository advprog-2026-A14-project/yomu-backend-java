package id.ac.ui.cs.advprog.yomubackendjava.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponseData {
    @JsonProperty("access_token")
    private final String accessToken;
    @JsonProperty("is_new_user")
    private final Boolean isNewUser;
    private final UserDto user;

    public AuthResponseData(String accessToken, UserDto user) {
        this.isNewUser = null;
        this.accessToken = accessToken;
        this.user = user;
    }

    public AuthResponseData(Boolean isNewUser, String accessToken, UserDto user) {
        this.isNewUser = isNewUser;
        this.accessToken = accessToken;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public Boolean getIsNewUser() {
        return isNewUser;
    }

    public UserDto getUser() {
        return user;
    }
}
