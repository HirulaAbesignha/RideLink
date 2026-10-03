package com.ridelink.account.api.dto;

import com.ridelink.account.domain.AccountRole;

import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UUID accountId,
        AccountRole role
) {
}
