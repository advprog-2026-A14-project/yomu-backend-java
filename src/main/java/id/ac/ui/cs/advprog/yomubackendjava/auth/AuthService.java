package id.ac.ui.cs.advprog.yomubackendjava.auth;

import id.ac.ui.cs.advprog.yomubackendjava.auth.dto.AuthResponseData;
import id.ac.ui.cs.advprog.yomubackendjava.auth.command.GoogleLoginCommand;
import id.ac.ui.cs.advprog.yomubackendjava.auth.command.LoginCommand;
import id.ac.ui.cs.advprog.yomubackendjava.auth.command.RegisterCommand;
import id.ac.ui.cs.advprog.yomubackendjava.auth.google.GoogleIdTokenVerifier;
import id.ac.ui.cs.advprog.yomubackendjava.auth.google.GoogleProfile;
import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.BadRequestException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.ConflictException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.ForbiddenException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.UnauthorizedException;
import id.ac.ui.cs.advprog.yomubackendjava.common.security.SecuritySanitizer;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.Role;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.UserEntity;
import id.ac.ui.cs.advprog.yomubackendjava.user.repo.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthResponseFactory authResponseFactory;
    private final IdentifierResolver identifierResolver;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final AuthUserSyncService authUserSyncService;
    private final GoogleUserProvisioningService googleUserProvisioningService;
    private final PasswordPolicy passwordPolicy;
    private final IdentifierNormalizer identifierNormalizer;
    private final AuthEventLogger authEventLogger;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthResponseFactory authResponseFactory,
            IdentifierResolver identifierResolver,
            GoogleIdTokenVerifier googleIdTokenVerifier,
            AuthUserSyncService authUserSyncService,
            GoogleUserProvisioningService googleUserProvisioningService,
            PasswordPolicy passwordPolicy,
            IdentifierNormalizer identifierNormalizer,
            AuthEventLogger authEventLogger
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authResponseFactory = authResponseFactory;
        this.identifierResolver = identifierResolver;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
        this.authUserSyncService = authUserSyncService;
        this.googleUserProvisioningService = googleUserProvisioningService;
        this.passwordPolicy = passwordPolicy;
        this.identifierNormalizer = identifierNormalizer;
        this.authEventLogger = authEventLogger;
    }

    public ApiResponse<AuthResponseData> registerLocal(RegisterCommand command) {
        String username = identifierNormalizer.username(command.username());
        String displayName = SecuritySanitizer.html(command.displayName());
        String email = identifierNormalizer.email(command.email());
        String phoneNumber = identifierNormalizer.phoneNumber(command.phoneNumber());

        passwordPolicy.validateNewPassword(command.password());
        validateRegisterIdentifiers(email, phoneNumber);
        validateUniqueness(username, email, phoneNumber);

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setPasswordHash(passwordEncoder.encode(command.password()));
        user.setRole(Role.PELAJAR);

        UserEntity savedUser;
        try {
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(GENERIC_CONFLICT_MESSAGE);
        }

        authUserSyncService.syncNewUser(savedUser.getUserId());
        authEventLogger.registerSuccess(savedUser.getUserId());

        AuthResponseData responseData = authResponseFactory.createLocalAuthResponse(savedUser);
        return ApiResponse.success(REGISTER_SUCCESS_MESSAGE, responseData);
    }

    public ApiResponse<AuthResponseData> loginLocal(LoginCommand command) {
        String identifier = identifierNormalizer.loginIdentifier(command.identifier());
        if (identifier == null) {
            authEventLogger.loginFailed("INVALID_CREDENTIAL");
            throw new UnauthorizedException(LOGIN_INVALID_CREDENTIALS_MESSAGE);
        }

        IdentifierResolver.ResolvedIdentifier resolvedIdentifier = identifierResolver.resolve(identifier);
        UserEntity user = assertActiveUserForLogin(resolvedIdentifier);
        if (user.getPasswordHash() == null) {
            authEventLogger.loginFailed("SSO_ONLY");
            throw new UnauthorizedException(LOGIN_SSO_ONLY_MESSAGE);
        }
        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            authEventLogger.loginFailed("INVALID_CREDENTIAL");
            throw new UnauthorizedException(LOGIN_INVALID_CREDENTIALS_MESSAGE);
        }

        AuthResponseData responseData = authResponseFactory.createLocalAuthResponse(user);
        authEventLogger.loginSuccess(user.getUserId());
        return ApiResponse.success(LOGIN_SUCCESS_MESSAGE, responseData);
    }

    public ApiResponse<AuthResponseData> googleLogin(GoogleLoginCommand command) {
        GoogleProfile profile = verifyGoogleToken(command.idToken());
        GoogleUserProvisioningService.ProvisionedGoogleUser provisionedUser =
                googleUserProvisioningService.findOrProvisionUser(command, profile);
        return buildGoogleLoginResponse(provisionedUser.user(), provisionedUser.isNewUser());
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
            authEventLogger.googleLoginFailed("INVALID_GOOGLE_TOKEN");
            throw new BadRequestException(GOOGLE_TOKEN_INVALID_MESSAGE);
        }
    }

    private ApiResponse<AuthResponseData> buildGoogleLoginResponse(UserEntity user, boolean isNewUser) {
        AuthResponseData responseData = authResponseFactory.createGoogleAuthResponse(user, isNewUser);
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
            authEventLogger.loginFailed("USER_DELETED");
            throw new ForbiddenException(LOGIN_DELETED_MESSAGE);
        }
        authEventLogger.loginFailed("INVALID_CREDENTIAL");
        throw new UnauthorizedException(LOGIN_INVALID_CREDENTIALS_MESSAGE);
    }
}
