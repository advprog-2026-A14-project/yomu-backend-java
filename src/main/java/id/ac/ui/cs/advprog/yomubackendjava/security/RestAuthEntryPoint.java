package id.ac.ui.cs.advprog.yomubackendjava.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAuthEntryPoint implements AuthenticationEntryPoint {
    private final SecurityJsonResponseWriter responseWriter;

    public RestAuthEntryPoint(SecurityJsonResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        responseWriter.writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }
}
