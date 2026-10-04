package com.ridelink.account.service;

import com.ridelink.account.api.dto.LoginRequest;
import com.ridelink.account.api.dto.LoginResponse;
import com.ridelink.account.api.error.ApiException;
import com.ridelink.account.domain.Account;
import com.ridelink.account.domain.AccountStatus;
import com.ridelink.account.repository.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthenticationService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthenticationService(AccountRepository accountRepository,
                                 PasswordEncoder passwordEncoder,
                                 JwtTokenService jwtTokenService) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(this::invalidCredentials);
        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw invalidCredentials();
        }
        if (account.getStatus() == AccountStatus.SUSPENDED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED",
                    "This account is suspended");
        }

        return new LoginResponse(
                jwtTokenService.createAccessToken(account),
                "Bearer",
                jwtTokenService.getExpirySeconds(),
                account.getId(),
                account.getRole());
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                "The email address or password is incorrect");
    }
}
