package id.ac.ui.cs.advprog.yomubackendjava.security;

import id.ac.ui.cs.advprog.yomubackendjava.user.domain.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public final class CurrentUser {
    private CurrentUser() {
    }

    public static Optional<UUID> userId() {
        return authentication()
                .map(Authentication::getPrincipal)
                .filter(JwtService.JwtClaims.class::isInstance)
                .map(JwtService.JwtClaims.class::cast)
                .map(JwtService.JwtClaims::userId);
    }

    public static Optional<Role> role() {
        return authentication()
                .map(Authentication::getPrincipal)
                .filter(JwtService.JwtClaims.class::isInstance)
                .map(JwtService.JwtClaims.class::cast)
                .map(JwtService.JwtClaims::role);
    }

    private static Optional<Authentication> authentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }
}
