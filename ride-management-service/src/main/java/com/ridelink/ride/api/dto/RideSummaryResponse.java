package com.ridelink.ride.api.dto;

import com.ridelink.ride.entity.RideStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record RideSummaryResponse(
        UUID rideId,
        UUID passengerId,
        UUID driverAccountId,
        RideStatus status,
        BigDecimal distanceKm,
        BigDecimal finalFare
) {
}
