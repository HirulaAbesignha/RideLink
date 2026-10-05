package com.ridelink.account.config;

import com.ridelink.account.domain.Account;
import com.ridelink.account.domain.AccountRole;
import com.ridelink.account.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String fullName;
    private final String phone;

    public AdminBootstrap(AccountRepository accountRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${bootstrap.admin.email:}") String email,
                          @Value("${bootstrap.admin.password:}") String password,
                          @Value("${bootstrap.admin.full-name:}") String fullName,
                          @Value("${bootstrap.admin.phone:}") String phone) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.phone = phone;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<String> values = List.of(email, password, fullName, phone);
        if (values.stream().allMatch(String::isBlank)) {
            return;
        }
        if (values.stream().anyMatch(String::isBlank)) {
            throw new IllegalStateException("All BOOTSTRAP_ADMIN values are required when bootstrap is enabled");
        }
        if (password.length() < 8 || password.length() > 72
                || !password.matches(".*[a-z].*")
                || !password.matches(".*[A-Z].*")
                || !password.matches(".*\\d.*")) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD does not meet the password policy");
        }

        String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
        accountRepository.findByEmail(normalizedEmail).ifPresentOrElse(existing -> {
            if (existing.getRole() != AccountRole.ADMIN) {
                throw new IllegalStateException("The bootstrap email belongs to a non-admin account");
            }
        }, () -> accountRepository.save(new Account(
                normalizedEmail,
                passwordEncoder.encode(password),
                AccountRole.ADMIN,
                fullName.strip(),
                phone.strip())));
    }
}
