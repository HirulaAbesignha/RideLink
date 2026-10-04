package com.ridelink.fare.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FinalFareResponse(
        UUID fareId,
        UUID rideId,
        UUID passengerId,
        BigDecimal distanceKm,
        BigDecimal amount,
        String currency,
        String ruleVersion,
        Instant createdAt
) {
}