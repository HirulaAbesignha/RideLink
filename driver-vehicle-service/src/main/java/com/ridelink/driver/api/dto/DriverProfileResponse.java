package com.ridelink.driver.api.dto;

import com.ridelink.driver.domain.DriverAvailability;

import java.math.BigDecimal;
import java.util.UUID;

public record DriverProfileResponse(
        UUID driverId,
        UUID accountId,
        String serviceArea,
        String locationName,
        BigDecimal latitude,
        BigDecimal longitude,
        DriverAvailability availability,
        VehicleResponse vehicle) {
}
