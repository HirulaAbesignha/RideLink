package com.ridelink.driver.api.dto;

import com.ridelink.driver.domain.DriverAvailability;
import jakarta.validation.constraints.NotNull;

public record UpdateAvailabilityRequest(@NotNull DriverAvailability availability) {
}
