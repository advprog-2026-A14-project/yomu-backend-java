package id.ac.ui.cs.advprog.yomubackendjava.auth.dto;

public class UserDto {
    private String userId;
    private String username;
    private String displayName;
    private String email;
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
