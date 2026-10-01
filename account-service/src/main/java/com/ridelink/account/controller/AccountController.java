package com.ridelink.account.controller;

import com.ridelink.account.api.dto.AccountResponse;
import com.ridelink.account.api.dto.AccountStatusResponse;
import com.ridelink.account.api.dto.RegisterAccountRequest;
import com.ridelink.account.api.dto.UpdateAccountProfileRequest;
import com.ridelink.account.api.dto.UpdateAccountStatusRequest;
import com.ridelink.account.api.error.ApiException;
import com.ridelink.account.domain.AccountRole;
import com.ridelink.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/passengers")
    public ResponseEntity<AccountResponse> registerPassenger(
            @Valid @RequestBody RegisterAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.register(request, AccountRole.PASSENGER));
    }

    @PostMapping("/drivers")
    public ResponseEntity<AccountResponse> registerDriver(
            @Valid @RequestBody RegisterAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.register(request, AccountRole.DRIVER));
    }

    @GetMapping("/me")
    public AccountResponse getOwnProfile(@AuthenticationPrincipal Jwt jwt) {
        return accountService.getOwnProfile(subject(jwt));
    }

    @PatchMapping("/me")
    public AccountResponse updateOwnProfile(@AuthenticationPrincipal Jwt jwt,
                                            @Valid @RequestBody UpdateAccountProfileRequest request) {
        return accountService.updateOwnProfile(subject(jwt), request);
    }

    @PatchMapping("/{accountId}/status")
    public AccountStatusResponse changeStatus(@PathVariable UUID accountId,
                                              @AuthenticationPrincipal Jwt jwt,
                                              @Valid @RequestBody UpdateAccountStatusRequest request) {
        return accountService.changeStatus(accountId, request.status(), subject(jwt));
    }

    private UUID subject(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN",
                    "The access token subject is invalid");
        }
    }
}
