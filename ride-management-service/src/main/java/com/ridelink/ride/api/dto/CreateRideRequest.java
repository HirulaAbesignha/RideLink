package com.ridelink.ride.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateRideRequest(
        @NotBlank @Size(max = 255) String pickup,
        @NotBlank @Size(max = 255) String destination,
        @NotBlank @Size(max = 80) String serviceArea,
        @NotNull @Digits(integer = 3, fraction = 2)
        @DecimalMin("0.10") @DecimalMax("100.00") BigDecimal distanceKm,
        @Min(1) @Max(12) int seatCount,
        @NotNull UUID fareEstimateId
) {
}
