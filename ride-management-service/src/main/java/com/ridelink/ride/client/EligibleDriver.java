package com.ridelink.ride.client;

import java.util.UUID;

public record EligibleDriver(
        UUID driverId,
        UUID accountId,
        String serviceArea,
        String locationName,
        String vehicleType,
        int seatCapacity
) {
}
