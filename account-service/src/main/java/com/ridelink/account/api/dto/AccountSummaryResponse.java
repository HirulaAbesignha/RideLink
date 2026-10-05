package com.ridelink.account.api.dto;

import com.ridelink.account.domain.AccountRole;
import com.ridelink.account.domain.AccountStatus;

import java.util.UUID;

public record AccountSummaryResponse(UUID accountId, AccountRole role, AccountStatus status) {
}
