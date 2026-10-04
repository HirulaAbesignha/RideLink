package com.ridelink.fare.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

public record CreateEstimateRequest(

        @NotBlank(message = "Pickup is required")
        @Size(max = 255, message = "Pickup must not exceed 255 characters")
        String pickup,

        @NotBlank(message = "Destination is required")
        @Size(max = 255, message = "Destination must not exceed 255 characters")
        String destination,

        @NotNull(message = "Distance is required")
        @Digits(integer = 3, fraction = 2, message = "Distance must have no more than two decimal places")
        @DecimalMin(value = "0.10", message = "Distance must be at least 0.10 km")
        @DecimalMax(value = "100.00", message = "Distance must not exceed 100.00 km")
        BigDecimal distanceKm

) {
}
