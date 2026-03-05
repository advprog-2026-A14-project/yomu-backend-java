package id.ac.ui.cs.advprog.yomubackendjava.user;

import id.ac.ui.cs.advprog.yomubackendjava.auth.AuthMapper;
import id.ac.ui.cs.advprog.yomubackendjava.auth.dto.UserDto;
import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.ForbiddenException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.UnauthorizedException;
import id.ac.ui.cs.advprog.yomubackendjava.security.CurrentUser;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.UserEntity;
import id.ac.ui.cs.advprog.yomubackendjava.user.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private static final String ME_SUCCESS_MESSAGE = "Profil pengguna berhasil diambil";
    private static final String UNAUTHORIZED_MESSAGE = "Unauthorized";
    private static final String DELETED_MESSAGE = "akun tidak aktif";

    private final UserRepository userRepository;
    private final AuthMapper authMapper;

    public UserService(UserRepository userRepository, AuthMapper authMapper) {
        this.userRepository = userRepository;
        this.authMapper = authMapper;
    }

    public ApiResponse<UserDto> me() {
        UUID userId = CurrentUser.userId().orElseThrow(() -> new UnauthorizedException(UNAUTHORIZED_MESSAGE));
        Optional<UserEntity> activeUser = userRepository.findByUserIdAndDeletedAtIsNull(userId);
        if (activeUser.isPresent()) {
            return ApiResponse.success(ME_SUCCESS_MESSAGE, authMapper.toUserDto(activeUser.get()));
        }

        if (userRepository.findById(userId).isPresent()) {
            throw new ForbiddenException(DELETED_MESSAGE);
        }
        throw new UnauthorizedException(UNAUTHORIZED_MESSAGE);
    }
}
