package id.ac.ui.cs.advprog.yomubackendjava.security;

import id.ac.ui.cs.advprog.yomubackendjava.auth.AuthEventLogger;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.ForbiddenException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    private static final int MAX_AUTHORIZATION_HEADER_LENGTH = 4096;

    private final JwtService jwtService;
    private final AuthenticatedUserStateValidator userStateValidator;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;
    private final AuthEventLogger authEventLogger;

    public JwtAuthFilter(
            JwtService jwtService,
            AuthenticatedUserStateValidator userStateValidator,
            RestAuthEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler,
            AuthEventLogger authEventLogger
    ) {
        this.jwtService = jwtService;
        this.userStateValidator = userStateValidator;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.authEventLogger = authEventLogger;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String token = extractBearerToken(authHeader);
                JwtService.JwtClaims claims = jwtService.parseAndValidate(token);
                userStateValidator.validate(claims);
                String authority = "ROLE_" + claims.role().name();
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        claims,
                        null,
                        List.of(new SimpleGrantedAuthority(authority))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (UnauthorizedException ex) {
                authEventLogger.jwtValidationFailed("JWT_INVALID");
                SecurityContextHolder.clearContext();
                authenticationEntryPoint.commence(
                        request,
                        response,
                        new InsufficientAuthenticationException(ex.getMessage(), ex)
                );
                return;
            } catch (ForbiddenException ex) {
                SecurityContextHolder.clearContext();
                accessDeniedHandler.handle(
                        request,
                        response,
                        new AccessDeniedException(ex.getMessage(), ex)
                );
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(String authHeader) {
        if (authHeader.length() > MAX_AUTHORIZATION_HEADER_LENGTH
                || !authHeader.startsWith(BEARER_PREFIX)
                || authHeader.length() == BEARER_PREFIX.length()) {
            throw new UnauthorizedException("Invalid authorization header");
        }
        return authHeader.substring(BEARER_PREFIX.length()).trim();
    }
}
