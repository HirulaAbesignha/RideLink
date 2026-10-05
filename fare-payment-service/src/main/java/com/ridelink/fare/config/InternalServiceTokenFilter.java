package com.ridelink.fare.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class InternalServiceTokenFilter extends OncePerRequestFilter {

    private final byte[] expectedToken;
    private final SecurityErrorWriter errorWriter;

    public InternalServiceTokenFilter(@Value("${security.internal.service-token}") String token,
                                      SecurityErrorWriter errorWriter) {
        if (token.length() < 32) {
            throw new IllegalArgumentException("SERVICE_TOKEN must contain at least 32 characters");
        }
        this.expectedToken = token.getBytes(StandardCharsets.UTF_8);
        this.errorWriter = errorWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String value = request.getHeader("X-Service-Token");
        byte[] supplied = value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedToken, supplied)) {
            errorWriter.write(request, response, 401, "INVALID_SERVICE_TOKEN",
                    "The service token is missing or invalid");
            return;
        }
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "internal-service", null, List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))));
        filterChain.doFilter(request, response);
    }
}
