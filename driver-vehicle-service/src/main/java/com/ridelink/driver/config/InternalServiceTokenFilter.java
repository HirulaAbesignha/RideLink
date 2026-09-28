package com.ridelink.driver.config;

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

    private static final String HEADER_NAME = "X-Service-Token";

    private final byte[] expectedToken;
    private final SecurityErrorWriter errorWriter;

    public InternalServiceTokenFilter(@Value("${security.internal.service-token}") String serviceToken,
                                      SecurityErrorWriter errorWriter) {
        if (serviceToken.length() < 32) {
            throw new IllegalArgumentException("SERVICE_TOKEN must contain at least 32 characters");
        }
        this.expectedToken = serviceToken.getBytes(StandardCharsets.UTF_8);
        this.errorWriter = errorWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String suppliedToken = request.getHeader(HEADER_NAME);
        byte[] suppliedBytes = suppliedToken == null
                ? new byte[0]
                : suppliedToken.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedToken, suppliedBytes)) {
            errorWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                    "INVALID_SERVICE_TOKEN", "The service token is missing or invalid");
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        "internal-service", null,
                        List.of(new SimpleGrantedAuthority("ROLE_SERVICE")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
