package com.ridelink.account.controller;

import com.ridelink.account.api.dto.AccountSummaryResponse;
import com.ridelink.account.service.AccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/accounts")
public class InternalAccountController {

    private final AccountService accountService;

    public InternalAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{accountId}/summary")
    public AccountSummaryResponse getSummary(@PathVariable UUID accountId) {
        return accountService.getSummary(accountId);
    }
}
