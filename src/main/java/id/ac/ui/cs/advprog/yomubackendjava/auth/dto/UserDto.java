package id.ac.ui.cs.advprog.yomubackendjava.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserDto {
    @JsonProperty("user_id")
    private String userId;
    private String username;

    @JsonProperty("display_name")
    private String displayName;

    private String email;

    @JsonProperty("phone_number")
    private String phoneNumber;

    private String role;

    public UserDto(
            String userId,
            String username,
            String displayName,
            String email,
            String phoneNumber,
            String role
    ) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.role = role;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getRole() {
        return role;
    }
}
