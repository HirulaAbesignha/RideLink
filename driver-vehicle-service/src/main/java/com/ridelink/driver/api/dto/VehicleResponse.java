package com.ridelink.driver.api.dto;

import com.ridelink.driver.domain.VehicleStatus;
import com.ridelink.driver.domain.VehicleType;

import java.util.UUID;

public record VehicleResponse(
        UUID vehicleId,
        String registrationNumber,
        VehicleType vehicleType,
        String manufacturer,
        String model,
        String colour,
        int seatCapacity,
        VehicleStatus status) {
}
