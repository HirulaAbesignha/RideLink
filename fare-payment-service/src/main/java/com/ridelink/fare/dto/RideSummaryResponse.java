package com.ridelink.fare.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RideSummaryResponse(
        UUID rideId,
        UUID passengerId,
        UUID driverAccountId,
        String status,
        BigDecimal distanceKm,
        BigDecimal finalFare) {
}