package com.ridelink.driver.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReservationRequest(@NotNull UUID rideId) {
}
