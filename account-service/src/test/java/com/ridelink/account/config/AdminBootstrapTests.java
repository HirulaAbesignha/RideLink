package com.ridelink.account.config;

import com.ridelink.account.domain.Account;
import com.ridelink.account.domain.AccountRole;
import com.ridelink.account.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBootstrapTests {

    @Test
    void createsAdminWithAnEncodedPasswordOnFirstRun() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(accountRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("AdminPass1")).thenReturn("bcrypt-value");
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminBootstrap bootstrap = new AdminBootstrap(
                accountRepository, passwordEncoder,
                " Admin@Example.com ", "AdminPass1", "RideLink Admin", "+94770000000");
        bootstrap.run(new DefaultApplicationArguments(new String[0]));

        var captor = org.mockito.ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        Account saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("admin@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("bcrypt-value");
        assertThat(saved.getRole()).isEqualTo(AccountRole.ADMIN);
    }

    @Test
    void refusesPartiallyConfiguredAdminBootstrap() {
        AdminBootstrap bootstrap = new AdminBootstrap(
                mock(AccountRepository.class), mock(PasswordEncoder.class),
                "admin@example.com", "", "RideLink Admin", "+94770000000");

        assertThatThrownBy(() -> bootstrap.run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("All BOOTSTRAP_ADMIN values");
    }
}
