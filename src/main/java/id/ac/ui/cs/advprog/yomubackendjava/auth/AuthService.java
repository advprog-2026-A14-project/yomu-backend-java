package id.ac.ui.cs.advprog.yomubackendjava.auth;

import id.ac.ui.cs.advprog.yomubackendjava.auth.dto.AuthResponseData;
import id.ac.ui.cs.advprog.yomubackendjava.auth.dto.GoogleLoginRequest;
import id.ac.ui.cs.advprog.yomubackendjava.auth.dto.LoginRequest;
import id.ac.ui.cs.advprog.yomubackendjava.auth.dto.RegisterRequest;
import id.ac.ui.cs.advprog.yomubackendjava.auth.google.GoogleIdTokenVerifier;
import id.ac.ui.cs.advprog.yomubackendjava.auth.google.GoogleProfile;
import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.BadRequestException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.ConflictException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.ForbiddenException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.UnauthorizedException;
import id.ac.ui.cs.advprog.yomubackendjava.integration.rust.RustEngineClient;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.OutboxService;
import id.ac.ui.cs.advprog.yomubackendjava.security.JwtService;
import id.ac.ui.cs.advprog.yomubackendjava.user.UserMapper;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.Role;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.UserEntity;
import id.ac.ui.cs.advprog.yomubackendjava.user.repo.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
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
    private static final String LOGIN_SUCCESS_MESSAGE = "Login berhasil";
    private static final String LOGIN_INVALID_CREDENTIALS_MESSAGE = "identifier atau password salah";
    private static final String LOGIN_DELETED_MESSAGE = "akun tidak aktif";
    private static final String LOGIN_SSO_ONLY_MESSAGE = "akun menggunakan metode login lain";
    private static final String GOOGLE_LOGIN_SUCCESS_MESSAGE = "Login Google berhasil";
    private static final String GOOGLE_TOKEN_INVALID_MESSAGE = "id_token tidak valid";
    private static final String GOOGLE_SUB_INVALID_MESSAGE = "google_sub tidak valid";
    private static final int RUST_SYNC_CREATED_STATUS = 201;
    private static final int RUST_SYNC_CONFLICT_STATUS = 409;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final IdentifierResolver identifierResolver;
    private final UsernameGenerator usernameGenerator;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final RustEngineClient rustEngineClient;
    private final OutboxService outboxService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserMapper userMapper,
            IdentifierResolver identifierResolver,
            UsernameGenerator usernameGenerator,
            GoogleIdTokenVerifier googleIdTokenVerifier,
            RustEngineClient rustEngineClient,
            OutboxService outboxService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.identifierResolver = identifierResolver;
        this.usernameGenerator = usernameGenerator;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
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
        AuthResponseData responseData = new AuthResponseData(accessToken, userMapper.toUserDto(savedUser));
        return ApiResponse.success(REGISTER_SUCCESS_MESSAGE, responseData);
    }

    public ApiResponse<AuthResponseData> loginLocal(LoginRequest request) {
        String identifier = normalize(request.getIdentifier());
        if (identifier == null) {
            throw new UnauthorizedException(LOGIN_INVALID_CREDENTIALS_MESSAGE);
        }

        IdentifierResolver.ResolvedIdentifier resolvedIdentifier = identifierResolver.resolve(identifier);
        UserEntity user = assertActiveUserForLogin(resolvedIdentifier);
        if (user.getPasswordHash() == null) {
            throw new UnauthorizedException(LOGIN_SSO_ONLY_MESSAGE);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException(LOGIN_INVALID_CREDENTIALS_MESSAGE);
        }

        String accessToken = jwtService.generateToken(user.getUserId(), user.getRole());
        AuthResponseData responseData = new AuthResponseData(accessToken, userMapper.toUserDto(user));
        return ApiResponse.success(LOGIN_SUCCESS_MESSAGE, responseData);
    }

    public ApiResponse<AuthResponseData> googleLogin(GoogleLoginRequest request) {
        GoogleProfile profile = verifyGoogleToken(request.getIdToken());
        String googleSub = normalize(profile.googleSub());
        if (googleSub == null) {
            throw new BadRequestException(GOOGLE_SUB_INVALID_MESSAGE);
        }

        Optional<UserEntity> existingUserOpt = userRepository.findByGoogleSub(googleSub);
        if (existingUserOpt.isPresent()) {
            UserEntity existingUser = existingUserOpt.get();
            if (existingUser.getDeletedAt() != null) {
                throw new ForbiddenException(LOGIN_DELETED_MESSAGE);
            }
            return buildGoogleLoginResponse(existingUser, false);
        }

        UserEntity newUser = buildNewGoogleUser(request, profile, googleSub);
        UserEntity savedUser;
        try {
            savedUser = userRepository.saveAndFlush(newUser);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(GENERIC_CONFLICT_MESSAGE);
        }

        syncUserToRust(savedUser.getUserId());
        return buildGoogleLoginResponse(savedUser, true);
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

    private GoogleProfile verifyGoogleToken(String idToken) {
        try {
            return googleIdTokenVerifier.verify(idToken);
        } catch (RuntimeException ex) {
            throw new BadRequestException(GOOGLE_TOKEN_INVALID_MESSAGE);
        }
    }

    private UserEntity buildNewGoogleUser(GoogleLoginRequest request, GoogleProfile profile, String googleSub) {
        String requestUsername = normalize(request.getUsername());
        String emailFromGoogle = normalize(profile.email());
        if (requestUsername != null && userRepository.findByUsernameAndDeletedAtIsNull(requestUsername).isPresent()) {
            throw new ConflictException(USERNAME_USED_MESSAGE);
        }
        if (emailFromGoogle != null && userRepository.findByEmailAndDeletedAtIsNull(emailFromGoogle).isPresent()) {
            throw new ConflictException(EMAIL_USED_MESSAGE);
        }

        String username = requestUsername != null
                ? requestUsername
                : usernameGenerator.generateFromEmail(emailFromGoogle);
        String displayName = resolveGoogleDisplayName(request, profile, username);

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setEmail(emailFromGoogle);
        user.setRole(Role.PELAJAR);
        user.setPasswordHash(null);
        user.setGoogleSub(googleSub);
        return user;
    }

    private String resolveGoogleDisplayName(GoogleLoginRequest request, GoogleProfile profile, String username) {
        String requestDisplayName = normalize(request.getDisplayName());
        if (requestDisplayName != null) {
            return requestDisplayName;
        }
        String googleName = normalize(profile.name());
        if (googleName != null) {
            return googleName;
        }
        return username;
    }

    private ApiResponse<AuthResponseData> buildGoogleLoginResponse(UserEntity user, boolean isNewUser) {
        String accessToken = jwtService.generateToken(user.getUserId(), user.getRole());
        AuthResponseData responseData = new AuthResponseData(isNewUser, accessToken, userMapper.toUserDto(user));
        return ApiResponse.success(GOOGLE_LOGIN_SUCCESS_MESSAGE, responseData);
    }

    private Optional<UserEntity> findActiveUser(IdentifierResolver.ResolvedIdentifier resolvedIdentifier) {
        return switch (resolvedIdentifier.type()) {
            case EMAIL -> userRepository.findByEmailAndDeletedAtIsNull(resolvedIdentifier.value());
            case PHONE_NUMBER -> userRepository.findByPhoneNumberAndDeletedAtIsNull(resolvedIdentifier.value());
            case USERNAME -> userRepository.findByUsernameAndDeletedAtIsNull(resolvedIdentifier.value());
        };
    }

    private Optional<UserEntity> findAnyUser(IdentifierResolver.ResolvedIdentifier resolvedIdentifier) {
        return switch (resolvedIdentifier.type()) {
            case EMAIL -> userRepository.findByEmail(resolvedIdentifier.value());
            case PHONE_NUMBER -> userRepository.findByPhoneNumber(resolvedIdentifier.value());
            case USERNAME -> userRepository.findByUsername(resolvedIdentifier.value());
        };
    }

    private UserEntity assertActiveUserForLogin(IdentifierResolver.ResolvedIdentifier resolvedIdentifier) {
        Optional<UserEntity> activeUserOpt = findActiveUser(resolvedIdentifier);
        if (activeUserOpt.isPresent()) {
            return activeUserOpt.get();
        }

        Optional<UserEntity> anyUser = findAnyUser(resolvedIdentifier);
        if (anyUser.isPresent() && anyUser.get().getDeletedAt() != null) {
            throw new ForbiddenException(LOGIN_DELETED_MESSAGE);
        }
        throw new UnauthorizedException(LOGIN_INVALID_CREDENTIALS_MESSAGE);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
