package com.ridelink.driver.client;

import java.util.UUID;

public record AccountSummary(UUID accountId, String role, String status) {
}
