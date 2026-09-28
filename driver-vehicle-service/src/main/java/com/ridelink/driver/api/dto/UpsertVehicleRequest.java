package com.ridelink.driver.api.dto;

import com.ridelink.driver.domain.VehicleStatus;
import com.ridelink.driver.domain.VehicleType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpsertVehicleRequest(
        @NotBlank @Size(max = 20)
        @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "must contain only letters, numbers and hyphens")
        String registrationNumber,
        @NotNull VehicleType vehicleType,
        @NotBlank @Size(max = 80) String manufacturer,
        @NotBlank @Size(max = 80) String model,
        @NotBlank @Size(max = 40) String colour,
        @Min(1) @Max(12) int seatCapacity,
        @NotNull VehicleStatus status) {
}
