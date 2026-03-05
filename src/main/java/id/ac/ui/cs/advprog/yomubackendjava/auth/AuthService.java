package id.ac.ui.cs.advprog.yomubackendjava.auth;

import id.ac.ui.cs.advprog.yomubackendjava.auth.dto.AuthResponseData;
import id.ac.ui.cs.advprog.yomubackendjava.auth.dto.RegisterRequest;
import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.BadRequestException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.ConflictException;
import id.ac.ui.cs.advprog.yomubackendjava.integration.rust.RustEngineClient;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.OutboxService;
import id.ac.ui.cs.advprog.yomubackendjava.security.JwtService;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.Role;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.UserEntity;
import id.ac.ui.cs.advprog.yomubackendjava.user.repo.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Service
public class AuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
    private static final String REGISTER_SUCCESS_MESSAGE = "Registrasi berhasil";
    private static final String IDENTIFIER_REQUIRED_MESSAGE = "email atau phone_number wajib diisi";
    private static final String USERNAME_USED_MESSAGE = "username sudah digunakan";
    private static final String EMAIL_USED_MESSAGE = "email sudah digunakan";
    private static final String PHONE_USED_MESSAGE = "phone_number sudah digunakan";
    private static final String GENERIC_CONFLICT_MESSAGE = "data sudah digunakan";
    private static final int RUST_SYNC_CREATED_STATUS = 201;
    private static final int RUST_SYNC_CONFLICT_STATUS = 409;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthMapper authMapper;
    private final RustEngineClient rustEngineClient;
    private final OutboxService outboxService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthMapper authMapper,
            RustEngineClient rustEngineClient,
            OutboxService outboxService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authMapper = authMapper;
        this.rustEngineClient = rustEngineClient;
        this.outboxService = outboxService;
    }

    public ApiResponse<AuthResponseData> registerLocal(RegisterRequest request) {
        String username = normalize(request.getUsername());
        String displayName = normalize(request.getDisplayName());
        String email = normalize(request.getEmail());
        String phoneNumber = normalize(request.getPhoneNumber());

        validateRegisterIdentifiers(email, phoneNumber);
        validateUniqueness(username, email, phoneNumber);

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.PELAJAR);

        UserEntity savedUser;
        try {
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(GENERIC_CONFLICT_MESSAGE);
        }

        syncUserToRust(savedUser.getUserId());

        String accessToken = jwtService.generateToken(savedUser.getUserId(), savedUser.getRole());
        AuthResponseData responseData = new AuthResponseData(accessToken, authMapper.toUserDto(savedUser));
        return ApiResponse.success(REGISTER_SUCCESS_MESSAGE, responseData);
    }

    private void syncUserToRust(UUID userId) {
        try {
            RustEngineClient.SyncResult syncResult = rustEngineClient.syncUser(userId);
            if (syncResult.statusCode() == RUST_SYNC_CREATED_STATUS
                    || syncResult.statusCode() == RUST_SYNC_CONFLICT_STATUS) {
                return;
            }

            String errorMessage = "status=" + syncResult.statusCode() + " body=" + syncResult.responseBody();
            LOGGER.error("Rust sync gagal untuk user_id={} {}", userId, errorMessage);
            outboxService.recordUserSyncFailure(userId, errorMessage);
        } catch (RestClientException ex) {
            LOGGER.error("Rust sync exception untuk user_id={}", userId, ex);
            outboxService.recordUserSyncFailure(userId, ex.getMessage());
        } catch (RuntimeException ex) {
            LOGGER.error("Rust sync runtime exception untuk user_id={}", userId, ex);
            outboxService.recordUserSyncFailure(userId, ex.getMessage());
        }
    }

    private void validateRegisterIdentifiers(String email, String phoneNumber) {
        if (email == null && phoneNumber == null) {
            throw new BadRequestException(IDENTIFIER_REQUIRED_MESSAGE);
        }
    }

    private void validateUniqueness(String username, String email, String phoneNumber) {
        if (userRepository.findByUsernameAndDeletedAtIsNull(username).isPresent()) {
            throw new ConflictException(USERNAME_USED_MESSAGE);
        }
        if (email != null && userRepository.findByEmailAndDeletedAtIsNull(email).isPresent()) {
            throw new ConflictException(EMAIL_USED_MESSAGE);
        }
        if (phoneNumber != null && userRepository.findByPhoneNumberAndDeletedAtIsNull(phoneNumber).isPresent()) {
            throw new ConflictException(PHONE_USED_MESSAGE);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
