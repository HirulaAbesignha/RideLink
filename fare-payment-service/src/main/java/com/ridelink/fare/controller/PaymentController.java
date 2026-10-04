package com.ridelink.fare.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.ridelink.fare.exception.FareApiException;

import com.ridelink.fare.dto.PaymentRequest;
import com.ridelink.fare.dto.PaymentResponse;
import com.ridelink.fare.dto.PaymentCreationResult;
import com.ridelink.fare.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payment Management", description = "Payment operations")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @Operation(summary = "Create a payment")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt) {

        java.util.UUID passengerId;
        try {
            java.util.UUID.fromString(idempotencyKey);
            passengerId = java.util.UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new FareApiException(400, "INVALID_IDENTIFIER", "Idempotency-Key and token subject must be UUIDs");
        }
        PaymentCreationResult result = paymentService.createPayment(request, idempotencyKey, passengerId);

        return ResponseEntity.status(result.replayed() ? 200 : 201).body(result.payment());
    }
}
