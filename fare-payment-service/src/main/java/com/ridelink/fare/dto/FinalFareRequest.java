package com.ridelink.fare.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;

public record FinalFareRequest(

        @NotNull(message = "Ride ID is required")
        UUID rideId,

        @NotNull(message = "Passenger ID is required")
        UUID passengerId,

        @NotNull(message = "Distance is required")
        @Digits(integer = 3, fraction = 2, message = "Distance must have no more than two decimal places")
        @DecimalMin(value = "0.10", message = "Distance must be at least 0.10 km")
        @DecimalMax(value = "100.00", message = "Distance must not exceed 100.00 km")
        BigDecimal distanceKm

) {
}
