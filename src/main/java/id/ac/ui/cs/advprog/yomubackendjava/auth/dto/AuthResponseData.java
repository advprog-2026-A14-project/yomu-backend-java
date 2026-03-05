package id.ac.ui.cs.advprog.yomubackendjava.auth.dto;

public class AuthResponseData {
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
