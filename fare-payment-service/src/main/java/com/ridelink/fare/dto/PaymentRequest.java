package com.ridelink.fare.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PaymentRequest(

        @NotNull(message = "Ride ID is required")
        UUID rideId,

        @NotBlank(message = "Simulation outcome is required")
        @Pattern(
                regexp = "SUCCESS|FAILURE",
                message = "Simulation outcome must be SUCCESS or FAILURE"
        )
        String simulationOutcome,

        @NotBlank(message = "Method label is required")
        @Pattern(
                regexp = "SIMULATED_CARD|SIMULATED_CASH",
                message = "Method label must be SIMULATED_CARD or SIMULATED_CASH"
        )
        String methodLabel

) {
}