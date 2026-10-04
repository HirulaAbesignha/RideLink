package com.ridelink.account.repository;

import com.ridelink.account.domain.AccountStatusAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountStatusAuditRepository extends JpaRepository<AccountStatusAudit, UUID> {
    long countByAccountId(UUID accountId);
}
