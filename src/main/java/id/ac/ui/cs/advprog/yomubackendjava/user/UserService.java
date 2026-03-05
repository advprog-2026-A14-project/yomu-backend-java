package id.ac.ui.cs.advprog.yomubackendjava.user;

import com.fasterxml.jackson.annotation.JsonProperty;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private static final String ME_SUCCESS_MESSAGE = "Profil pengguna berhasil diambil";
    private static final String PROFILE_UPDATED_SUCCESS_MESSAGE = "Profil berhasil diperbarui";
    private static final String PASSWORD_UPDATED_SUCCESS_MESSAGE = "Password berhasil diperbarui";
    private static final String IDENTIFIERS_UPDATED_SUCCESS_MESSAGE = "Login identifier berhasil diperbarui";
    private static final String ACCOUNT_DELETED_SUCCESS_MESSAGE = "Akun berhasil dihapus";
    private static final String BATCH_SUCCESS_MESSAGE = "Batch pengguna berhasil diambil";
    private static final String UNAUTHORIZED_MESSAGE = "Unauthorized";
    private static final String DELETED_MESSAGE = "akun tidak aktif";
    private static final String PROFILE_EMPTY_PAYLOAD_MESSAGE = "minimal username atau display_name harus diisi";
    private static final String IDENTIFIERS_EMPTY_PAYLOAD_MESSAGE = "minimal email atau phone_number harus diisi";
    private static final String USERNAME_USED_MESSAGE = "username sudah digunakan";
    private static final String EMAIL_USED_MESSAGE = "email sudah digunakan";
    private static final String PHONE_USED_MESSAGE = "phone_number sudah digunakan";
    private static final String CURRENT_PASSWORD_INVALID_MESSAGE = "current_password salah";
    private static final String IDS_REQUIRED_MESSAGE = "ids wajib diisi";
    private static final String IDS_MAX_MESSAGE = "maksimal 100 ids";
    private static final String IDS_INVALID_UUID_MESSAGE = "ids harus UUID valid";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public ApiResponse<UserDto> me() {
        return ApiResponse.success(ME_SUCCESS_MESSAGE, userMapper.toUserDto(assertActiveUser()));
    }

    public ApiResponse<UserDto> updateProfile(UpdateProfileRequest request) {
        String username = normalize(request.getUsername());
        String displayName = normalize(request.getDisplayName());
        validateProfilePayload(username, displayName);

        UserEntity user = assertActiveUser();
        ensureUsernameAvailable(user, username);
        applyProfileUpdates(user, username, displayName);

        UserEntity updatedUser = userRepository.saveAndFlush(user);
        return ApiResponse.success(PROFILE_UPDATED_SUCCESS_MESSAGE, userMapper.toUserDto(updatedUser));
    }

    public ApiResponse<Void> updatePassword(UpdatePasswordRequest request) {
        UserEntity user = assertActiveUser();
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

        UserEntity user = assertActiveUser();
        ensureEmailAvailable(user, email);
        ensurePhoneAvailable(user, phoneNumber);
        applyIdentifierUpdates(user, email, phoneNumber);

        UserEntity updatedUser = userRepository.saveAndFlush(user);
        return ApiResponse.success(IDENTIFIERS_UPDATED_SUCCESS_MESSAGE, userMapper.toUserDto(updatedUser));
    }

    public ApiResponse<Void> deleteAccount() {
        UserEntity user = assertActiveUser();
        user.setDeletedAt(Instant.now());
        userRepository.saveAndFlush(user);
        return ApiResponse.success(ACCOUNT_DELETED_SUCCESS_MESSAGE);
    }

    public ApiResponse<UserBatchResponseData> batchUsers(String idsParam) {
        List<UUID> userIds = parseBatchIds(idsParam);
        List<UserEntity> foundUsers = userRepository.findByUserIdInAndDeletedAtIsNull(userIds);
        Map<UUID, UserEntity> foundById = new HashMap<>();
        for (UserEntity foundUser : foundUsers) {
            foundById.put(foundUser.getUserId(), foundUser);
        }

        List<UserBatchItem> users = new ArrayList<>();
        List<String> notFoundIds = new ArrayList<>();
        for (UUID userId : userIds) {
            UserEntity found = foundById.get(userId);
            if (found == null) {
                notFoundIds.add(userId.toString());
            } else {
                users.add(new UserBatchItem(
                        found.getUserId().toString(),
                        found.getUsername(),
                        found.getDisplayName()
                ));
            }
        }

        return ApiResponse.success(BATCH_SUCCESS_MESSAGE, new UserBatchResponseData(users, notFoundIds));
    }

    private List<UUID> parseBatchIds(String idsParam) {
        String normalized = normalize(idsParam);
        if (normalized == null) {
            throw new BadRequestException(IDS_REQUIRED_MESSAGE);
        }

        String[] rawIds = normalized.split(",");
        if (rawIds.length == 0) {
            throw new BadRequestException(IDS_REQUIRED_MESSAGE);
        }
        if (rawIds.length > 100) {
            throw new BadRequestException(IDS_MAX_MESSAGE);
        }

        List<UUID> parsedIds = new ArrayList<>();
        for (String rawId : rawIds) {
            String trimmed = normalize(rawId);
            if (trimmed == null) {
                throw new BadRequestException(IDS_INVALID_UUID_MESSAGE);
            }
            try {
                parsedIds.add(UUID.fromString(trimmed));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException(IDS_INVALID_UUID_MESSAGE);
            }
        }
        return parsedIds;
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

    private UserEntity assertActiveUser() {
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

    public record UserBatchResponseData(
            List<UserBatchItem> users,
            @JsonProperty("not_found_ids") List<String> notFoundIds
    ) {
    }

    public record UserBatchItem(
            @JsonProperty("user_id") String userId,
            String username,
            @JsonProperty("display_name") String displayName
    ) {
    }
}
