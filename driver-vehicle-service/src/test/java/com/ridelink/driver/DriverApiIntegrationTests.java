package com.ridelink.driver;

import com.ridelink.driver.client.AccountClient;
import com.ridelink.driver.client.AccountSummary;
import com.ridelink.driver.domain.DriverAvailability;
import com.ridelink.driver.repository.DriverProfileRepository;
import com.ridelink.driver.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DriverApiIntegrationTests {

    private static final String SERVICE_TOKEN = "test-service-token-with-at-least-32-characters";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DriverProfileRepository driverRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @MockitoBean
    private AccountClient accountClient;

    @BeforeEach
    void clearDatabase() {
        vehicleRepository.deleteAll();
        driverRepository.deleteAll();
    }

    @Test
    void driverCanCreateProfileAndManageVehicleAndAvailability() throws Exception {
        UUID accountId = UUID.randomUUID();
        activeDriverAccount(accountId);

        mockMvc.perform(post("/api/v1/drivers/me/profile")
                        .with(driverJwt(accountId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileBody(" colombo ")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.serviceArea").value("COLOMBO"))
                .andExpect(jsonPath("$.availability").value("OFFLINE"));

        mockMvc.perform(put("/api/v1/drivers/me/vehicle")
                        .with(driverJwt(accountId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleBody("CAB-1234", "ACTIVE", 4)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vehicle.registrationNumber").value("CAB-1234"));

        mockMvc.perform(patch("/api/v1/drivers/me/availability")
                        .with(driverJwt(accountId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availability\":\"AVAILABLE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availability").value("AVAILABLE"));

        mockMvc.perform(get("/api/v1/drivers/me").with(driverJwt(accountId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicle.seatCapacity").value(4));
    }

    @Test
    void activeVehicleIsRequiredBeforeGoingAvailable() throws Exception {
        UUID accountId = UUID.randomUUID();
        createProfile(accountId);

        mockMvc.perform(patch("/api/v1/drivers/me/availability")
                        .with(driverJwt(accountId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availability\":\"AVAILABLE\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ACTIVE_VEHICLE_REQUIRED"));
    }

    @Test
    void validationAndRoleChecksReturnContractErrors() throws Exception {
        UUID accountId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/drivers/me/profile")
                        .with(jwt().jwt(token -> token.subject(accountId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_PASSENGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileBody("COLOMBO")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(post("/api/v1/drivers/me/profile")
                        .with(driverJwt(accountId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serviceArea":"", "locationName":"", "latitude":91, "longitude":79}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
    }

    @Test
    void internalEndpointsRejectMissingOrWrongServiceToken() throws Exception {
        mockMvc.perform(get("/internal/v1/drivers/eligible")
                        .queryParam("serviceArea", "COLOMBO")
                        .queryParam("seatCount", "2"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_SERVICE_TOKEN"));

        mockMvc.perform(get("/internal/v1/drivers/eligible")
                        .header("X-Service-Token", "wrong-token")
                        .queryParam("serviceArea", "COLOMBO")
                        .queryParam("seatCount", "2"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_SERVICE_TOKEN"));
    }

    @Test
    void rideServiceCanFindReserveReplayAndReleaseDriver() throws Exception {
        UUID accountId = UUID.randomUUID();
        createAvailableDriver(accountId);
        UUID driverId = driverRepository.findByAccountId(accountId).orElseThrow().getId();
        UUID rideId = UUID.randomUUID();

        mockMvc.perform(get("/internal/v1/drivers/eligible")
                        .header("X-Service-Token", SERVICE_TOKEN)
                        .queryParam("serviceArea", "colombo")
                        .queryParam("seatCount", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.drivers.length()").value(1))
                .andExpect(jsonPath("$.drivers[0].driverId").value(driverId.toString()));

        mockMvc.perform(post("/internal/v1/drivers/{driverId}/reservations", driverId)
                        .header("X-Service-Token", SERVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rideId\":\"" + rideId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.availability").value("RESERVED"));

        mockMvc.perform(post("/internal/v1/drivers/{driverId}/reservations", driverId)
                        .header("X-Service-Token", SERVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rideId\":\"" + rideId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rideId").value(rideId.toString()));

        mockMvc.perform(post("/internal/v1/drivers/{driverId}/reservations", driverId)
                        .header("X-Service-Token", SERVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rideId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DRIVER_NOT_AVAILABLE"));

        mockMvc.perform(delete("/internal/v1/drivers/{driverId}/reservations/{rideId}",
                        driverId, UUID.randomUUID())
                        .header("X-Service-Token", SERVICE_TOKEN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVATION_MISMATCH"));

        mockMvc.perform(delete("/internal/v1/drivers/{driverId}/reservations/{rideId}", driverId, rideId)
                        .header("X-Service-Token", SERVICE_TOKEN))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/internal/v1/drivers/{driverId}/reservations/{rideId}", driverId, rideId)
                        .header("X-Service-Token", SERVICE_TOKEN))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(
                driverRepository.findById(driverId).orElseThrow().getAvailability())
                .isEqualTo(DriverAvailability.AVAILABLE);
    }

    @Test
    void reservedDriverCannotReplaceVehicleOrChangeAvailability() throws Exception {
        UUID accountId = UUID.randomUUID();
        createAvailableDriver(accountId);
        UUID driverId = driverRepository.findByAccountId(accountId).orElseThrow().getId();

        mockMvc.perform(post("/internal/v1/drivers/{driverId}/reservations", driverId)
                        .header("X-Service-Token", SERVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rideId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/v1/drivers/me/vehicle")
                        .with(driverJwt(accountId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleBody("CAB-9999", "ACTIVE", 4)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DRIVER_RESERVED"));

        mockMvc.perform(patch("/api/v1/drivers/me/availability")
                        .with(driverJwt(accountId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availability\":\"OFFLINE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DRIVER_RESERVED"));
    }

    private void createAvailableDriver(UUID accountId) throws Exception {
        createProfile(accountId);
        mockMvc.perform(put("/api/v1/drivers/me/vehicle")
                        .with(driverJwt(accountId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleBody("CAB-" + accountId.toString().substring(0, 4), "ACTIVE", 4)))
                .andExpect(status().isCreated());
        mockMvc.perform(patch("/api/v1/drivers/me/availability")
                        .with(driverJwt(accountId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availability\":\"AVAILABLE\"}"))
                .andExpect(status().isOk());
    }

    private void createProfile(UUID accountId) throws Exception {
        activeDriverAccount(accountId);
        mockMvc.perform(post("/api/v1/drivers/me/profile")
                        .with(driverJwt(accountId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileBody("COLOMBO")))
                .andExpect(status().isCreated());
    }

    private void activeDriverAccount(UUID accountId) {
        when(accountClient.getAccountSummary(org.mockito.ArgumentMatchers.eq(accountId),
                org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new AccountSummary(accountId, "DRIVER", "ACTIVE"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor driverJwt(UUID accountId) {
        return jwt()
                .jwt(token -> token.subject(accountId.toString()).issuer("ridelink-account-service"))
                .authorities(new SimpleGrantedAuthority("ROLE_DRIVER"));
    }

    private String profileBody(String serviceArea) {
        return """
                {
                  "serviceArea":"%s",
                  "locationName":"Bambalapitiya",
                  "latitude":6.8935,
                  "longitude":79.8552
                }
                """.formatted(serviceArea);
    }

    private String vehicleBody(String registration, String status, int seats) {
        return """
                {
                  "registrationNumber":"%s",
                  "vehicleType":"CAR",
                  "manufacturer":"Toyota",
                  "model":"Aqua",
                  "colour":"White",
                  "seatCapacity":%d,
                  "status":"%s"
                }
                """.formatted(registration, seats, status);
    }
}
