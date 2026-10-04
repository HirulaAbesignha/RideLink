package com.ridelink.ride.config;

import com.ridelink.ride.api.error.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response,
                      int status, String code, String message) throws IOException {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        String correlationId = value == null ? "unknown" : value.toString();
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
                Instant.now(), status, code, message, request.getRequestURI(), correlationId, List.of()));
    }
}
