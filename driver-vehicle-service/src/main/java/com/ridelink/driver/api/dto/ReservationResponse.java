package com.ridelink.driver.api.dto;

import com.ridelink.driver.domain.DriverAvailability;

import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
        UUID driverId,
        UUID rideId,
        DriverAvailability availability,
        Instant reservedAt) {
}
