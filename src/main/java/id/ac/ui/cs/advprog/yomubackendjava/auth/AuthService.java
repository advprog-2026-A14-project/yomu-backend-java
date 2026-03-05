package id.ac.ui.cs.advprog.yomubackendjava.auth;

import id.ac.ui.cs.advprog.yomubackendjava.auth.dto.AuthResponseData;
import id.ac.ui.cs.advprog.yomubackendjava.auth.dto.RegisterRequest;
import id.ac.ui.cs.advprog.yomubackendjava.auth.dto.UserDto;
import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private static final String REGISTER_SUCCESS_MESSAGE = "Registrasi berhasil";

    public ApiResponse<AuthResponseData> registerLocal(RegisterRequest request) {
        UserDto userDto = new UserDto(
                "stub-user-id",
                request.getUsername(),
                request.getDisplayName(),
                request.getEmail(),
                request.getPhoneNumber(),
                "PELAJAR"
        );
        return ApiResponse.success(REGISTER_SUCCESS_MESSAGE, new AuthResponseData("stub-token", userDto));
    }
}
