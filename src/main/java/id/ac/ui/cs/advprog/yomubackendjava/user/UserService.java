package id.ac.ui.cs.advprog.yomubackendjava.user;

import id.ac.ui.cs.advprog.yomubackendjava.auth.dto.UserDto;
import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.BadRequestException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.ConflictException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.ForbiddenException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.UnauthorizedException;
import id.ac.ui.cs.advprog.yomubackendjava.security.CurrentUser;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.UserEntity;
import id.ac.ui.cs.advprog.yomubackendjava.user.dto.UpdateIdentifiersRequest;
import id.ac.ui.cs.advprog.yomubackendjava.user.dto.UpdatePasswordRequest;
import id.ac.ui.cs.advprog.yomubackendjava.user.dto.UpdateProfileRequest;
import id.ac.ui.cs.advprog.yomubackendjava.user.repo.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private static final String ME_SUCCESS_MESSAGE = "Profil pengguna berhasil diambil";
    private static final String PROFILE_UPDATED_SUCCESS_MESSAGE = "Profil berhasil diperbarui";
    private static final String PASSWORD_UPDATED_SUCCESS_MESSAGE = "Password berhasil diperbarui";
    private static final String IDENTIFIERS_UPDATED_SUCCESS_MESSAGE = "Login identifier berhasil diperbarui";
    private static final String UNAUTHORIZED_MESSAGE = "Unauthorized";
    private static final String DELETED_MESSAGE = "akun tidak aktif";
    private static final String PROFILE_EMPTY_PAYLOAD_MESSAGE = "minimal username atau display_name harus diisi";
    private static final String IDENTIFIERS_EMPTY_PAYLOAD_MESSAGE = "minimal email atau phone_number harus diisi";
    private static final String USERNAME_USED_MESSAGE = "username sudah digunakan";
    private static final String EMAIL_USED_MESSAGE = "email sudah digunakan";
    private static final String PHONE_USED_MESSAGE = "phone_number sudah digunakan";
    private static final String CURRENT_PASSWORD_INVALID_MESSAGE = "current_password salah";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public ApiResponse<UserDto> me() {
        return ApiResponse.success(ME_SUCCESS_MESSAGE, userMapper.toUserDto(getCurrentActiveUser()));
    }

    public ApiResponse<UserDto> updateProfile(UpdateProfileRequest request) {
        String username = normalize(request.getUsername());
        String displayName = normalize(request.getDisplayName());
        validateProfilePayload(username, displayName);

        UserEntity user = getCurrentActiveUser();
        ensureUsernameAvailable(user, username);
        applyProfileUpdates(user, username, displayName);

        UserEntity updatedUser = userRepository.saveAndFlush(user);
        return ApiResponse.success(PROFILE_UPDATED_SUCCESS_MESSAGE, userMapper.toUserDto(updatedUser));
    }

    public ApiResponse<Void> updatePassword(UpdatePasswordRequest request) {
        UserEntity user = getCurrentActiveUser();
        if (user.getPasswordHash() != null) {
            String currentPassword = normalize(request.getCurrentPassword());
            if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
                throw new UnauthorizedException(CURRENT_PASSWORD_INVALID_MESSAGE);
            }
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.saveAndFlush(user);
        return ApiResponse.success(PASSWORD_UPDATED_SUCCESS_MESSAGE);
    }

    public ApiResponse<UserDto> updateLoginIdentifiers(UpdateIdentifiersRequest request) {
        String email = normalize(request.getEmail());
        String phoneNumber = normalize(request.getPhoneNumber());
        validateIdentifiersPayload(email, phoneNumber);

        UserEntity user = getCurrentActiveUser();
        ensureEmailAvailable(user, email);
        ensurePhoneAvailable(user, phoneNumber);
        applyIdentifierUpdates(user, email, phoneNumber);

        UserEntity updatedUser = userRepository.saveAndFlush(user);
        return ApiResponse.success(IDENTIFIERS_UPDATED_SUCCESS_MESSAGE, userMapper.toUserDto(updatedUser));
    }

    private void validateProfilePayload(String username, String displayName) {
        if (username == null && displayName == null) {
            throw new BadRequestException(PROFILE_EMPTY_PAYLOAD_MESSAGE);
        }
    }

    private void validateIdentifiersPayload(String email, String phoneNumber) {
        if (email == null && phoneNumber == null) {
            throw new BadRequestException(IDENTIFIERS_EMPTY_PAYLOAD_MESSAGE);
        }
    }

    private void ensureUsernameAvailable(UserEntity user, String username) {
        if (username != null && !username.equals(user.getUsername()) && usernameAlreadyUsed(username)) {
            throw new ConflictException(USERNAME_USED_MESSAGE);
        }
    }

    private void ensureEmailAvailable(UserEntity user, String email) {
        if (email != null && !email.equals(user.getEmail()) && emailAlreadyUsed(email)) {
            throw new ConflictException(EMAIL_USED_MESSAGE);
        }
    }

    private void ensurePhoneAvailable(UserEntity user, String phoneNumber) {
        if (phoneNumber != null && !phoneNumber.equals(user.getPhoneNumber()) && phoneAlreadyUsed(phoneNumber)) {
            throw new ConflictException(PHONE_USED_MESSAGE);
        }
    }

    private void applyProfileUpdates(UserEntity user, String username, String displayName) {
        if (username != null) {
            user.setUsername(username);
        }
        if (displayName != null) {
            user.setDisplayName(displayName);
        }
    }

    private void applyIdentifierUpdates(UserEntity user, String email, String phoneNumber) {
        if (email != null) {
            user.setEmail(email);
        }
        if (phoneNumber != null) {
            user.setPhoneNumber(phoneNumber);
        }
    }

    private UserEntity getCurrentActiveUser() {
        UUID userId = CurrentUser.userId().orElseThrow(() -> new UnauthorizedException(UNAUTHORIZED_MESSAGE));
        Optional<UserEntity> activeUser = userRepository.findByUserIdAndDeletedAtIsNull(userId);
        if (activeUser.isPresent()) {
            return activeUser.get();
        }

        if (userRepository.findById(userId).isPresent()) {
            throw new ForbiddenException(DELETED_MESSAGE);
        }
        throw new UnauthorizedException(UNAUTHORIZED_MESSAGE);
    }

    private boolean usernameAlreadyUsed(String username) {
        return userRepository.findByUsernameAndDeletedAtIsNull(username).isPresent();
    }

    private boolean emailAlreadyUsed(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email).isPresent();
    }

    private boolean phoneAlreadyUsed(String phoneNumber) {
        return userRepository.findByPhoneNumberAndDeletedAtIsNull(phoneNumber).isPresent();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
