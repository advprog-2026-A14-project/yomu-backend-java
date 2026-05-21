package id.ac.ui.cs.advprog.yomubackendjava.auth;

import id.ac.ui.cs.advprog.yomubackendjava.auth.dto.AuthResponseData;
import id.ac.ui.cs.advprog.yomubackendjava.auth.command.GoogleLoginCommand;
import id.ac.ui.cs.advprog.yomubackendjava.auth.command.LoginCommand;
import id.ac.ui.cs.advprog.yomubackendjava.auth.command.RegisterCommand;
import id.ac.ui.cs.advprog.yomubackendjava.auth.dto.GoogleLoginRequest;
import id.ac.ui.cs.advprog.yomubackendjava.auth.dto.LoginRequest;
import id.ac.ui.cs.advprog.yomubackendjava.auth.dto.RegisterRequest;
import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseData>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.registerLocal(toCommand(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseData>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginLocal(toCommand(request)));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponseData>> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.googleLogin(toCommand(request)));
    }

    private RegisterCommand toCommand(RegisterRequest request) {
        return new RegisterCommand(
                request.getUsername(),
                request.getDisplayName(),
                request.getPassword(),
                request.getEmail(),
                request.getPhoneNumber()
        );
    }

    private LoginCommand toCommand(LoginRequest request) {
        return new LoginCommand(request.getIdentifier(), request.getPassword());
    }

    private GoogleLoginCommand toCommand(GoogleLoginRequest request) {
        return new GoogleLoginCommand(request.getIdToken(), request.getUsername(), request.getDisplayName());
    }
}
