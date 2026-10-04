package com.ridelink.fare.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EstimateResponse(
        UUID estimateId,
        String pickup,
        String destination,
        BigDecimal distanceKm,
        BigDecimal amount,
        String currency,
        String ruleVersion,
        Instant createdAt,
        Instant expiresAt
) {
}