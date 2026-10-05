package com.ridelink.fare.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.ridelink.fare.dto.ReceiptResponse;
import com.ridelink.fare.exception.FareApiException;
import com.ridelink.fare.service.ReceiptService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Receipt Management", description = "Payment receipt operations")
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @GetMapping("/{paymentId}/receipt")
    @Operation(summary = "Get payment receipt")
    public ResponseEntity<ReceiptResponse> getReceipt(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(
                receiptService.getReceipt(paymentId, accountId(jwt),
                        "ADMIN".equals(jwt.getClaimAsString("role"))));
    }

    private UUID accountId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new FareApiException(401, "INVALID_TOKEN", "The token subject must be a UUID");
        }
    }
}
