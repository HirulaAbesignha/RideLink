package com.ridelink.ride.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelRideRequest(@NotBlank @Size(max = 255) String reason) {
}
