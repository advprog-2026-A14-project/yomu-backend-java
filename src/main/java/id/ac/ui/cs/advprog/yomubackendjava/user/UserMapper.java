package id.ac.ui.cs.advprog.yomubackendjava.user;

import id.ac.ui.cs.advprog.yomubackendjava.auth.dto.UserDto;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserDto toUserDto(UserEntity user) {
        return new UserDto(
                user.getUserId().toString(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole().name()
        );
    }
}
