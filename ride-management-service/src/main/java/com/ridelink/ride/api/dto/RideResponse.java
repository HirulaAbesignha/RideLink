package com.ridelink.ride.api.dto;

import com.ridelink.ride.entity.RideStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RideResponse(
        UUID rideId,
        UUID passengerId,
        UUID driverId,
        UUID driverAccountId,
        String pickup,
        String destination,
        String serviceArea,
        BigDecimal distanceKm,
        int seatCount,
        UUID fareEstimateId,
        BigDecimal estimatedFare,
        BigDecimal finalFare,
        String currency,
        RideStatus status,
        String cancellationReason,
        Instant acceptedAt,
        Instant startedAt,
        Instant completedAt,
        Instant cancelledAt,
        Instant createdAt,
        Instant updatedAt
) {
}
