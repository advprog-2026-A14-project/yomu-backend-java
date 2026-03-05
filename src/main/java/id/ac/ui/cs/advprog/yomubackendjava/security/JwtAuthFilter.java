package id.ac.ui.cs.advprog.yomubackendjava.security;

import id.ac.ui.cs.advprog.yomubackendjava.common.exception.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthFilter(JwtService jwtService, RestAuthEntryPoint authenticationEntryPoint) {
        this.jwtService = jwtService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            try {
                jwtService.parseAndValidate(token);
            } catch (UnauthorizedException ex) {
                authenticationEntryPoint.commence(
                        request,
                        response,
                        new InsufficientAuthenticationException(ex.getMessage(), ex)
                );
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
