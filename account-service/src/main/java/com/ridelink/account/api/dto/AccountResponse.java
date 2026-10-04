package com.ridelink.account.api.dto;

import com.ridelink.account.domain.AccountRole;
import com.ridelink.account.domain.AccountStatus;

import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID accountId,
        String email,
        AccountRole role,
        AccountStatus status,
        String fullName,
        String phone,
        Instant createdAt
) {
}
