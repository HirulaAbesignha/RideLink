package com.ridelink.fare.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        UUID rideId,
        BigDecimal amount,
        String currency,
        String status,
        String methodLabel,
        Instant recordedAt
) {
}