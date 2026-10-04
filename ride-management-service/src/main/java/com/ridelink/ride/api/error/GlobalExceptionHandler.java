package com.ridelink.ride.api.error;

import com.ridelink.ride.config.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiErrorResponse> handleApi(ApiException exception, HttpServletRequest request) {
        return build(exception.getStatus(), exception.getCode(), exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception,
                                                       HttpServletRequest request) {
        List<FieldErrorResponse> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::fieldError).toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "One or more fields are invalid",
                request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiErrorResponse> handleConstraint(ConstraintViolationException exception,
                                                       HttpServletRequest request) {
        List<FieldErrorResponse> errors = exception.getConstraintViolations().stream()
                .map(value -> new FieldErrorResponse(value.getPropertyPath().toString(), value.getMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "One or more fields are invalid",
                request, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleMalformed(HttpMessageNotReadableException exception,
                                                      HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_JSON", "The request body could not be read",
                request, List.of());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingRequestHeaderException.class})
    ResponseEntity<ApiErrorResponse> handleIdentifier(Exception exception, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_IDENTIFIER", "A required identifier is missing or invalid",
                request, List.of());
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    ResponseEntity<ApiErrorResponse> handleConflict(Exception exception, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT",
                "The request conflicts with a concurrent or previous request", request, List.of());
    }

    private FieldErrorResponse fieldError(FieldError error) {
        return new FieldErrorResponse(error.getField(), error.getDefaultMessage());
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String code, String message,
                                                   HttpServletRequest request,
                                                   List<FieldErrorResponse> errors) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        String correlationId = value == null ? "unknown" : value.toString();
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(), status.value(), code, message, request.getRequestURI(), correlationId, errors));
    }
}
