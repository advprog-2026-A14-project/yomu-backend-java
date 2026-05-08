package id.ac.ui.cs.advprog.yomubackendjava.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "hanya boleh berisi huruf, angka, titik, underscore, dan dash")
    private String username;

    @NotBlank
    @Size(max = 100)
    @JsonProperty("display_name")
    private String displayName;

    @NotBlank
    @Size(min = 8, max = 128)
    private String password;

    @Email
    @Size(max = 254)
    private String email;

    @JsonProperty("phone_number")
    @Size(max = 32)
    @Pattern(regexp = "^$|^\\+?[0-9][0-9 .-]{5,30}$", message = "tidak valid")
    private String phoneNumber;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
