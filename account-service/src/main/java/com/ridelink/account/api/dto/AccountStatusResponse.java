package com.ridelink.account.api.dto;

import com.ridelink.account.domain.AccountRole;
import com.ridelink.account.domain.AccountStatus;

import java.time.Instant;
import java.util.UUID;

public record AccountStatusResponse(
        UUID accountId,
        AccountRole role,
        AccountStatus status,
        Instant updatedAt
) {
}
