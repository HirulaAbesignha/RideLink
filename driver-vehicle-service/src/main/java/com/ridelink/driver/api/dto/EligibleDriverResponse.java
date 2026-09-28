package com.ridelink.driver.api.dto;

import com.ridelink.driver.domain.VehicleType;

import java.util.UUID;

public record EligibleDriverResponse(
        UUID driverId,
        UUID accountId,
        String serviceArea,
        String locationName,
        VehicleType vehicleType,
        int seatCapacity) {
}
