package com.ridelink.fare.exception;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.ridelink.fare.config.CorrelationIdFilter;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(FareApiException.class)
    ResponseEntity<ApiError> handleFare(FareApiException ex, HttpServletRequest request) {
        return response(ex.getStatus(), ex.getCode(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage())).toList();
        return response(400, "VALIDATION_FAILED", "One or more fields are invalid", request, fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        return response(400, "VALIDATION_FAILED", "One or more fields are invalid", request, List.of());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiError> handleMalformedRequest(Exception ex, HttpServletRequest request) {
        return response(400, "INVALID_IDENTIFIER", "Request body or identifier is invalid", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        return response(500, "INTERNAL_ERROR", "The request could not be completed", request, List.of());
    }

    private ResponseEntity<ApiError> response(int status, String code, String message,
            HttpServletRequest request, List<FieldError> errors) {
        String correlationId = request.getHeader("X-Correlation-Id");
        if (correlationId == null) {
            Object attribute = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
            if (attribute instanceof String value) correlationId = value;
        }
        if (correlationId == null || correlationId.isBlank()) correlationId = UUID.randomUUID().toString();
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status, code, message,
                request.getRequestURI(), correlationId, errors));
    }

    public record ApiError(Instant timestamp, int status, String code, String message,
            String path, String correlationId, List<FieldError> fieldErrors) { }
    public record FieldError(String field, String message) { }
}
