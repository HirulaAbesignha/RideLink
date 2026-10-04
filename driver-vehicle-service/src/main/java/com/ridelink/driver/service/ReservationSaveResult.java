package com.ridelink.driver.service;

import com.ridelink.driver.api.dto.ReservationResponse;

public record ReservationSaveResult(ReservationResponse reservation, boolean created) {
}
