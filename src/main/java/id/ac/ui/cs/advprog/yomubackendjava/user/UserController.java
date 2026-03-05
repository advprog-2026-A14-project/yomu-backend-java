package id.ac.ui.cs.advprog.yomubackendjava.user;

import id.ac.ui.cs.advprog.yomubackendjava.auth.dto.UserDto;
import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import id.ac.ui.cs.advprog.yomubackendjava.user.dto.UpdateIdentifiersRequest;
import id.ac.ui.cs.advprog.yomubackendjava.user.dto.UpdatePasswordRequest;
import id.ac.ui.cs.advprog.yomubackendjava.user.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> me() {
        return ResponseEntity.ok(userService.me());
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(@RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        return ResponseEntity.ok(userService.updatePassword(request));
    }

    @PatchMapping("/me/login-identifiers")
    public ResponseEntity<ApiResponse<UserDto>> updateLoginIdentifiers(@Valid @RequestBody UpdateIdentifiersRequest request) {
        return ResponseEntity.ok(userService.updateLoginIdentifiers(request));
    }
}
