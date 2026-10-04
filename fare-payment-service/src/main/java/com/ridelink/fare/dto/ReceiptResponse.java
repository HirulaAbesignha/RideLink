package com.ridelink.fare.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReceiptResponse(
        String receiptNumber,
        UUID paymentId,
        UUID rideId,
        UUID passengerId,
        BigDecimal amount,
        String currency,
        String paymentStatus,
        String methodLabel,
        Instant issuedAt,
        String notice
) {
}