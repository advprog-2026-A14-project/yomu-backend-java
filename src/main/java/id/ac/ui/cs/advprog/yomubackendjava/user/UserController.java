package id.ac.ui.cs.advprog.yomubackendjava.user;

import id.ac.ui.cs.advprog.yomubackendjava.auth.dto.UserDto;
import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import id.ac.ui.cs.advprog.yomubackendjava.user.dto.UpdateIdentifiersRequest;
import id.ac.ui.cs.advprog.yomubackendjava.user.dto.UpdatePasswordRequest;
import id.ac.ui.cs.advprog.yomubackendjava.user.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/batch")
    public ResponseEntity<ApiResponse<UserService.UserBatchResponseData>> batchUsers(@RequestParam("ids") String ids) {
        return ResponseEntity.ok(userService.batchUsers(ids));
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

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteAccount() {
        return ResponseEntity.ok(userService.deleteAccount());
    }
}
