package com.ridelink.fare.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InternalServiceTokenFilter extends OncePerRequestFilter {

    private final String expectedToken;

    public InternalServiceTokenFilter(@Value("${service.token}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String supplied = request.getHeader("X-Service-Token");
        boolean valid = supplied != null && MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":401,\"code\":\"INVALID_SERVICE_TOKEN\",\"message\":\"A valid service token is required\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
