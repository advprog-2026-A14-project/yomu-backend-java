package id.ac.ui.cs.advprog.yomubackendjava.auth;

import id.ac.ui.cs.advprog.yomubackendjava.auth.command.GoogleLoginCommand;
import id.ac.ui.cs.advprog.yomubackendjava.auth.google.GoogleProfile;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.BadRequestException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.ConflictException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.ForbiddenException;
import id.ac.ui.cs.advprog.yomubackendjava.common.security.SecuritySanitizer;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.Role;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.UserEntity;
import id.ac.ui.cs.advprog.yomubackendjava.user.repo.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GoogleUserProvisioningService {
    private static final String USERNAME_USED_MESSAGE = "username sudah digunakan";
    private static final String EMAIL_USED_MESSAGE = "email sudah digunakan";
    private static final String GENERIC_CONFLICT_MESSAGE = "data sudah digunakan";
    private static final String LOGIN_DELETED_MESSAGE = "akun tidak aktif";
    private static final String GOOGLE_SUB_INVALID_MESSAGE = "google_sub tidak valid";

    private final UserRepository userRepository;
    private final UsernameGenerator usernameGenerator;
    private final AuthUserSyncService authUserSyncService;

    public GoogleUserProvisioningService(
            UserRepository userRepository,
            UsernameGenerator usernameGenerator,
            AuthUserSyncService authUserSyncService
    ) {
        this.userRepository = userRepository;
        this.usernameGenerator = usernameGenerator;
        this.authUserSyncService = authUserSyncService;
    }

    public ProvisionedGoogleUser findOrProvisionUser(GoogleLoginCommand command, GoogleProfile profile) {
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
            return new ProvisionedGoogleUser(existingUser, false);
        }

        UserEntity savedUser = saveNewGoogleUser(buildNewGoogleUser(command, profile, googleSub));
        authUserSyncService.syncNewUser(savedUser.getUserId());
        return new ProvisionedGoogleUser(savedUser, true);
    }

    private UserEntity saveNewGoogleUser(UserEntity user) {
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(GENERIC_CONFLICT_MESSAGE);
        }
    }

    private UserEntity buildNewGoogleUser(GoogleLoginCommand command, GoogleProfile profile, String googleSub) {
        String requestUsername = normalize(command.username());
        String emailFromGoogle = normalize(profile.email());
        validateGoogleUserUniqueness(requestUsername, emailFromGoogle);

        String username = requestUsername != null
                ? requestUsername
                : usernameGenerator.generateFromEmail(emailFromGoogle);
        String displayName = resolveGoogleDisplayName(command, profile, username);

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setEmail(emailFromGoogle);
        user.setRole(Role.PELAJAR);
        user.setPasswordHash(null);
        user.setGoogleSub(googleSub);
        return user;
    }

    private void validateGoogleUserUniqueness(String username, String email) {
        if (username != null && userRepository.findByUsernameAndDeletedAtIsNull(username).isPresent()) {
            throw new ConflictException(USERNAME_USED_MESSAGE);
        }
        if (email != null && userRepository.findByEmailAndDeletedAtIsNull(email).isPresent()) {
            throw new ConflictException(EMAIL_USED_MESSAGE);
        }
    }

    private String resolveGoogleDisplayName(GoogleLoginCommand command, GoogleProfile profile, String username) {
        String requestDisplayName = SecuritySanitizer.html(command.displayName());
        if (requestDisplayName != null) {
            return requestDisplayName;
        }
        String googleName = SecuritySanitizer.html(profile.name());
        if (googleName != null) {
            return googleName;
        }
        return username;
    }

    private String normalize(String value) {
        return SecuritySanitizer.normalize(value);
    }

    public record ProvisionedGoogleUser(UserEntity user, boolean isNewUser) {
    }
}
