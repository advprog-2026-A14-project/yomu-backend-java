package id.ac.ui.cs.advprog.yomubackendjava.auth;

import id.ac.ui.cs.advprog.yomubackendjava.auth.dto.AuthResponseData;
import id.ac.ui.cs.advprog.yomubackendjava.security.JwtService;
import id.ac.ui.cs.advprog.yomubackendjava.user.UserMapper;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class AuthResponseFactory {
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthResponseFactory(JwtService jwtService, UserMapper userMapper) {
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    public AuthResponseData createLocalAuthResponse(UserEntity user) {
        return new AuthResponseData(createAccessToken(user), userMapper.toUserDto(user));
    }

    public AuthResponseData createGoogleAuthResponse(UserEntity user, boolean isNewUser) {
        return new AuthResponseData(isNewUser, createAccessToken(user), userMapper.toUserDto(user));
    }

    private String createAccessToken(UserEntity user) {
        return jwtService.generateToken(user.getUserId(), user.getRole(), user.getTokenVersion());
    }
}
