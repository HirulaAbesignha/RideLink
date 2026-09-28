package com.ridelink.driver.client;

import java.util.UUID;

public interface AccountClient {
    AccountSummary getAccountSummary(UUID accountId, String correlationId);
}
