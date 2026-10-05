package com.ridelink.ride;

import com.ridelink.ride.api.error.ApiException;
import com.ridelink.ride.client.DriverServiceClient;
import com.ridelink.ride.client.EligibleDriver;
import com.ridelink.ride.client.FareEstimateResponse;
import com.ridelink.ride.client.FareServiceClient;
import com.ridelink.ride.client.FinalFareResponse;
import com.ridelink.ride.entity.RideStatus;
import com.ridelink.ride.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RideApiIntegrationTests {

    private static final String SERVICE_TOKEN =
            "test-service-token-that-is-longer-than-thirty-two-characters";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FareServiceClient fareClient;

    @MockitoBean
    private DriverServiceClient driverClient;

    @BeforeEach
    void clearDatabase() {
        rideRepository.deleteAll();
    }

    @Test
    void completeRideFlowUsesFareAndDriverContracts() throws Exception {
        UUID passengerId = UUID.randomUUID();
        UUID driverAccountId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID estimateId = UUID.randomUUID();
        stubEstimate(estimateId);
        when(driverClient.findEligible("COLOMBO", 2)).thenReturn(List.of(
                new EligibleDriver(driverId, driverAccountId, "COLOMBO", "Fort", "CAR", 4)));
        when(driverClient.reserve(eq(driverId), any(UUID.class))).thenReturn(true);

        MvcResult created = mockMvc.perform(post("/api/v1/rides")
                        .with(userJwt(passengerId, "PASSENGER"))
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(estimateId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passengerId").value(passengerId.toString()))
                .andExpect(jsonPath("$.driverId").value(driverId.toString()))
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andReturn();
        UUID rideId = UUID.fromString(json(created, "rideId"));

        mockMvc.perform(post("/api/v1/rides/{rideId}/accept", rideId)
                        .with(userJwt(driverAccountId, "DRIVER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
        mockMvc.perform(post("/api/v1/rides/{rideId}/start", rideId)
                        .with(userJwt(driverAccountId, "DRIVER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        when(fareClient.createFinalFare(rideId, passengerId, new BigDecimal("10.50")))
                .thenReturn(new FinalFareResponse(UUID.randomUUID(), rideId, passengerId,
                        new BigDecimal("10.50"), new BigDecimal("1090.00"), "LKR",
                        "RIDELINK_2026_V1", Instant.now()));
        mockMvc.perform(post("/api/v1/rides/{rideId}/complete", rideId)
                        .with(userJwt(driverAccountId, "DRIVER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.finalFare").value(1090.00));

        verify(driverClient).release(driverId, rideId);
        mockMvc.perform(get("/internal/v1/rides/{rideId}/summary", rideId)
                        .header("X-Service-Token", SERVICE_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverAccountId").value(driverAccountId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void noDriverKeepsRequestedRideForLaterReview() throws Exception {
        UUID estimateId = UUID.randomUUID();
        stubEstimate(estimateId);
        when(driverClient.findEligible("COLOMBO", 2)).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/rides")
                        .with(userJwt(UUID.randomUUID(), "PASSENGER"))
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(estimateId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_AVAILABLE_DRIVER"));

        assertThat(rideRepository.findAll()).singleElement()
                .extracting(ride -> ride.getStatus()).isEqualTo(RideStatus.REQUESTED);
    }

    @Test
    void idempotencyReplayReturnsSameRideAndChangedRequestConflicts() throws Exception {
        UUID passengerId = UUID.randomUUID();
        UUID estimateId = UUID.randomUUID();
        UUID key = UUID.randomUUID();
        stubEstimate(estimateId);
        when(driverClient.findEligible("COLOMBO", 2)).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/rides")
                        .with(userJwt(passengerId, "PASSENGER"))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody(estimateId)))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/rides")
                        .with(userJwt(passengerId, "PASSENGER"))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody(estimateId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUESTED"));

        String changed = createBody(estimateId).replace("\"seatCount\":2", "\"seatCount\":3");
        mockMvc.perform(post("/api/v1/rides")
                        .with(userJwt(passengerId, "PASSENGER"))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(changed))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    void roleOwnershipAndInternalTokenAreEnforced() throws Exception {
        mockMvc.perform(post("/api/v1/rides")
                        .with(userJwt(UUID.randomUUID(), "DRIVER"))
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(UUID.randomUUID())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/internal/v1/rides/{rideId}/summary", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_SERVICE_TOKEN"));
    }

    @Test
    void fareFailureKeepsRideInProgress() throws Exception {
        UUID passengerId = UUID.randomUUID();
        UUID driverAccountId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID estimateId = UUID.randomUUID();
        stubEstimate(estimateId);
        when(driverClient.findEligible("COLOMBO", 2)).thenReturn(List.of(
                new EligibleDriver(driverId, driverAccountId, "COLOMBO", "Fort", "CAR", 4)));
        when(driverClient.reserve(eq(driverId), any(UUID.class))).thenReturn(true);
        MvcResult created = mockMvc.perform(post("/api/v1/rides")
                        .with(userJwt(passengerId, "PASSENGER"))
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(createBody(estimateId)))
                .andReturn();
        UUID rideId = UUID.fromString(json(created, "rideId"));
        mockMvc.perform(post("/api/v1/rides/{id}/accept", rideId).with(userJwt(driverAccountId, "DRIVER")));
        mockMvc.perform(post("/api/v1/rides/{id}/start", rideId).with(userJwt(driverAccountId, "DRIVER")));
        when(fareClient.createFinalFare(eq(rideId), eq(passengerId), any(BigDecimal.class)))
                .thenThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                        "FARE_SERVICE_UNAVAILABLE", "Fare Service is unavailable"));

        mockMvc.perform(post("/api/v1/rides/{id}/complete", rideId)
                        .with(userJwt(driverAccountId, "DRIVER")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("FARE_SERVICE_UNAVAILABLE"));
        assertThat(rideRepository.findById(rideId).orElseThrow().getStatus())
                .isEqualTo(RideStatus.IN_PROGRESS);
    }

    @Test
    void passengerCanListAndCancelAnAssignedRide() throws Exception {
        UUID passengerId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID estimateId = UUID.randomUUID();
        stubEstimate(estimateId);
        when(driverClient.findEligible("COLOMBO", 2)).thenReturn(List.of(
                new EligibleDriver(driverId, UUID.randomUUID(), "COLOMBO", "Fort", "CAR", 4)));
        when(driverClient.reserve(eq(driverId), any(UUID.class))).thenReturn(true);

        MvcResult created = mockMvc.perform(post("/api/v1/rides")
                        .with(userJwt(passengerId, "PASSENGER"))
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(createBody(estimateId)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID rideId = UUID.fromString(json(created, "rideId"));

        mockMvc.perform(get("/api/v1/rides?status=ASSIGNED&page=0&size=20")
                        .with(userJwt(passengerId, "PASSENGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].rideId").value(rideId.toString()));
        mockMvc.perform(get("/api/v1/rides").with(userJwt(UUID.randomUUID(), "PASSENGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(post("/api/v1/rides/{id}/cancel", rideId)
                        .with(userJwt(passengerId, "PASSENGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Plans changed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Plans changed"));

        verify(driverClient).release(driverId, rideId);
    }

    private void stubEstimate(UUID estimateId) {
        when(fareClient.getEstimate(estimateId)).thenReturn(new FareEstimateResponse(
                estimateId, "Wellawatte", "Colombo Fort", new BigDecimal("10.50"),
                new BigDecimal("1090.00"), "LKR", "RIDELINK_2026_V1",
                Instant.now(), Instant.now().plusSeconds(1800)));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor userJwt(UUID accountId, String role) {
        return jwt().jwt(token -> token.subject(accountId.toString()).claim("role", role))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private String createBody(UUID estimateId) {
        return """
                {
                  "pickup":"Wellawatte",
                  "destination":"Colombo Fort",
                  "serviceArea":"COLOMBO",
                  "distanceKm":10.50,
                  "seatCount":2,
                  "fareEstimateId":"%s"
                }
                """.formatted(estimateId);
    }

    private String json(MvcResult result, String field) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray()).get(field).asText();
    }
}
