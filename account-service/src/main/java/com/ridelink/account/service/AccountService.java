package com.ridelink.account.service;

import com.ridelink.account.api.dto.AccountResponse;
import com.ridelink.account.api.dto.AccountStatusResponse;
import com.ridelink.account.api.dto.AccountSummaryResponse;
import com.ridelink.account.api.dto.RegisterAccountRequest;
import com.ridelink.account.api.dto.UpdateAccountProfileRequest;
import com.ridelink.account.api.error.ApiException;
import com.ridelink.account.domain.Account;
import com.ridelink.account.domain.AccountRole;
import com.ridelink.account.domain.AccountStatus;
import com.ridelink.account.domain.AccountStatusAudit;
import com.ridelink.account.repository.AccountRepository;
import com.ridelink.account.repository.AccountStatusAuditRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountStatusAuditRepository auditRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accountRepository,
                          AccountStatusAuditRepository auditRepository,
                          PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.auditRepository = auditRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AccountResponse register(RegisterAccountRequest request, AccountRole role) {
        String email = normalizeEmail(request.email());
        if (accountRepository.existsByEmail(email)) {
            throw emailAlreadyExists();
        }

        Account account = new Account(
                email,
                passwordEncoder.encode(request.password()),
                role,
                request.fullName().strip(),
                request.phone().strip());
        try {
            return toResponse(accountRepository.saveAndFlush(account));
        } catch (DataIntegrityViolationException exception) {
            // The database protects against two registrations arriving together.
            throw emailAlreadyExists();
        }
    }

    @Transactional(readOnly = true)
    public AccountResponse getOwnProfile(UUID accountId) {
        return toResponse(requireAccount(accountId));
    }

    @Transactional
    public AccountResponse updateOwnProfile(UUID accountId, UpdateAccountProfileRequest request) {
        Account account = requireAccount(accountId);
        account.updateProfile(request.fullName().strip(), request.phone().strip());
        return toResponse(account);
    }

    @Transactional
    public AccountStatusResponse changeStatus(UUID accountId, AccountStatus newStatus, UUID adminId) {
        Account admin = requireAccount(adminId);
        if (admin.getRole() != AccountRole.ADMIN || admin.getStatus() != AccountStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                    "An active administrator account is required");
        }

        Account account = requireAccount(accountId);
        AccountStatus previousStatus = account.getStatus();
        if (previousStatus != newStatus) {
            account.changeStatus(newStatus);
            auditRepository.save(new AccountStatusAudit(
                    account.getId(), admin.getId(), previousStatus, newStatus));
        }
        return new AccountStatusResponse(
                account.getId(), account.getRole(), account.getStatus(), account.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public AccountSummaryResponse getSummary(UUID accountId) {
        Account account = requireAccount(accountId);
        return new AccountSummaryResponse(account.getId(), account.getRole(), account.getStatus());
    }

    @Transactional(readOnly = true)
    public Account requireAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                        "The account was not found"));
    }

    private String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private ApiException emailAlreadyExists() {
        return new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS",
                "An account already exists for this email address");
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(), account.getEmail(), account.getRole(), account.getStatus(),
                account.getFullName(), account.getPhone(), account.getCreatedAt());
    }
}
