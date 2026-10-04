package com.ridelink.account;

import com.ridelink.account.domain.Account;
import com.ridelink.account.domain.AccountRole;
import com.ridelink.account.domain.AccountStatus;
import com.ridelink.account.repository.AccountRepository;
import com.ridelink.account.repository.AccountStatusAuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountApiIntegrationTests {

    private static final String SERVICE_TOKEN =
            "test-service-token-that-is-longer-than-thirty-two-characters";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountStatusAuditRepository auditRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @BeforeEach
    void clearDatabase() {
        auditRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void passengerCanRegisterLoginReadAndUpdateOwnProfile() throws Exception {
        String email = "Passenger@Example.com";
        String password = "StrongPass1";

        MvcResult registration = mockMvc.perform(post("/api/v1/accounts/passengers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(email, password, "Nimal Perera", "+94771234567")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.email").value("passenger@example.com"))
                .andExpect(jsonPath("$.role").value("PASSENGER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        UUID accountId = UUID.fromString(json(registration, "accountId"));
        Account saved = accountRepository.findById(accountId).orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, saved.getPasswordHash())).isTrue();

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email.toLowerCase(), password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600))
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andReturn();

        String token = json(login, "accessToken");
        Jwt jwt = jwtDecoder.decode(token);
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("ridelink-account-service");
        assertThat(jwt.getSubject()).isEqualTo(accountId.toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("PASSENGER");
        assertThat(jwt.getId()).isNotBlank();
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).getSeconds()).isEqualTo(3600);

        mockMvc.perform(get("/api/v1/accounts/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Nimal Perera"));

        mockMvc.perform(patch("/api/v1/accounts/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Nimal Silva","phone":"+94770000000"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Nimal Silva"))
                .andExpect(jsonPath("$.phone").value("+94770000000"));
    }

    @Test
    void driverRoleIsAssignedByServerAndRoleInjectionIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(
                                "driver@example.com", "DriverPass1", "Kamal Silva", "+94771111111")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("DRIVER"));

        mockMvc.perform(post("/api/v1/accounts/passengers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"attacker@example.com",
                                  "password":"StrongPass1",
                                  "fullName":"Bad Actor",
                                  "phone":"+94772222222",
                                  "role":"ADMIN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    }

    @Test
    void duplicateEmailAndWeakPasswordReturnContractErrors() throws Exception {
        String body = registrationBody(
                "duplicate@example.com", "StrongPass1", "First Person", "+94773333333");
        mockMvc.perform(post("/api/v1/accounts/passengers")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/accounts/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(
                                "DUPLICATE@example.com", "OtherPass2", "Second Person", "+94774444444")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));

        mockMvc.perform(post("/api/v1/accounts/passengers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(
                                "weak@example.com", "password", "Weak Password", "+94775555555")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')]").exists());
    }

    @Test
    void wrongCredentialsAndSuspendedAccountCannotLogin() throws Exception {
        Account passenger = saveAccount("blocked@example.com", "CorrectPass1", AccountRole.PASSENGER);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(passenger.getEmail(), "WrongPass1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        passenger.changeStatus(AccountStatus.SUSPENDED);
        accountRepository.save(passenger);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(passenger.getEmail(), "CorrectPass1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_SUSPENDED"));
    }

    @Test
    void adminCanChangeStatusAndChangeIsAudited() throws Exception {
        Account admin = saveAccount("admin@example.com", "AdminPass1", AccountRole.ADMIN);
        Account passenger = saveAccount("member@example.com", "MemberPass1", AccountRole.PASSENGER);
        String adminToken = loginToken(admin.getEmail(), "AdminPass1");

        mockMvc.perform(patch("/api/v1/accounts/{accountId}/status", passenger.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(passenger.getId().toString()))
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        assertThat(auditRepository.countByAccountId(passenger.getId())).isEqualTo(1);

        String passengerToken = loginTokenForActivePassenger();
        mockMvc.perform(patch("/api/v1/accounts/{accountId}/status", admin.getId())
                        .header("Authorization", "Bearer " + passengerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void internalSummaryRequiresServiceTokenAndDoesNotReturnPersonalData() throws Exception {
        Account driver = saveAccount("private@example.com", "DriverPass1", AccountRole.DRIVER);

        mockMvc.perform(get("/internal/v1/accounts/{accountId}/summary", driver.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_SERVICE_TOKEN"));

        mockMvc.perform(get("/internal/v1/accounts/{accountId}/summary", driver.getId())
                        .header("X-Service-Token", "wrong-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_SERVICE_TOKEN"));

        mockMvc.perform(get("/internal/v1/accounts/{accountId}/summary", driver.getId())
                        .header("X-Service-Token", SERVICE_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(driver.getId().toString()))
                .andExpect(jsonPath("$.role").value("DRIVER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.fullName").doesNotExist())
                .andExpect(jsonPath("$.phone").doesNotExist());
    }

    @Test
    void protectedProfileRequiresAValidToken() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(get("/api/v1/accounts/me")
                        .header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));

        Instant now = Instant.now();
        JwtClaimsSet expiredClaims = JwtClaimsSet.builder()
                .issuer("ridelink-account-service")
                .subject(UUID.randomUUID().toString())
                .issuedAt(now.minusSeconds(7200))
                .expiresAt(now.minusSeconds(3600))
                .id(UUID.randomUUID().toString())
                .claim("role", "PASSENGER")
                .build();
        String expiredToken = jwtEncoder.encode(JwtEncoderParameters.from(expiredClaims)).getTokenValue();
        mockMvc.perform(get("/api/v1/accounts/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
    }

    private Account saveAccount(String email, String password, AccountRole role) {
        return accountRepository.save(new Account(
                email, passwordEncoder.encode(password), role, "Test Account", "+94776666666"));
    }

    private String loginTokenForActivePassenger() throws Exception {
        Account passenger = saveAccount("active@example.com", "ActivePass1", AccountRole.PASSENGER);
        return loginToken(passenger.getEmail(), "ActivePass1");
    }

    private String loginToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return json(result, "accessToken");
    }

    private String json(MvcResult result, String field) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray())
                .get(field).asText();
    }

    private String registrationBody(String email, String password, String fullName, String phone) {
        return """
                {
                  "email":"%s",
                  "password":"%s",
                  "fullName":"%s",
                  "phone":"%s"
                }
                """.formatted(email, password, fullName, phone);
    }

    private String loginBody(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }
}
