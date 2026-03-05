package id.ac.ui.cs.advprog.yomubackendjava.user.repo;

import id.ac.ui.cs.advprog.yomubackendjava.user.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByUserIdAndDeletedAtIsNull(UUID userId);

    Optional<UserEntity> findByUsernameAndDeletedAtIsNull(String username);

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmailAndDeletedAtIsNull(String email);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByPhoneNumberAndDeletedAtIsNull(String phoneNumber);

    Optional<UserEntity> findByPhoneNumber(String phoneNumber);

    Optional<UserEntity> findByGoogleSubAndDeletedAtIsNull(String googleSub);

    Optional<UserEntity> findByGoogleSub(String googleSub);
}
