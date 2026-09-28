package com.ridelink.driver.service;

import com.ridelink.driver.api.dto.DriverProfileResponse;

public record VehicleSaveResult(DriverProfileResponse profile, boolean created) {
}
