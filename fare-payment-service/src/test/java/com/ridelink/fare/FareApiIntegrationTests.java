package com.ridelink.fare;

import com.ridelink.fare.client.RideServiceClient;
import com.ridelink.fare.dto.RideSummaryResponse;
import com.ridelink.fare.repository.FareEstimateRepository;
import com.ridelink.fare.repository.FinalFareRepository;
import com.ridelink.fare.repository.PaymentRepository;
import com.ridelink.fare.repository.ReceiptRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FareApiIntegrationTests {

    private static final String SERVICE_TOKEN =
            "test-only-service-token-placeholder-with-32-characters";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReceiptRepository receiptRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private FinalFareRepository finalFareRepository;

    @Autowired
    private FareEstimateRepository fareEstimateRepository;

    @MockitoBean
    private RideServiceClient rideServiceClient;

    @BeforeEach
    void clearDatabase() {
        receiptRepository.deleteAll();
        paymentRepository.deleteAll();
        finalFareRepository.deleteAll();
        fareEstimateRepository.deleteAll();
    }

    @Test
    void estimateUsesPublishedFareRuleAndCanBeValidatedInternally() throws Exception {
        UUID passengerId = UUID.randomUUID();
        MvcResult result = mockMvc.perform(post("/api/v1/fares/estimates")
                        .with(userJwt(passengerId, "PASSENGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pickup":"Wellawatte","destination":"Colombo Fort","distanceKm":10.50}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(1090.00))
                .andExpect(jsonPath("$.currency").value("LKR"))
                .andExpect(jsonPath("$.ruleVersion").value("RIDELINK_2026_V1"))
                .andReturn();

        UUID estimateId = UUID.fromString(json(result, "estimateId"));
        mockMvc.perform(get("/internal/v1/fares/estimates/{id}", estimateId)
                        .header("X-Service-Token", SERVICE_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimateId").value(estimateId.toString()));
    }

    @Test
    void finalFareCreationIsIdempotentAndRejectsChangedData() throws Exception {
        UUID rideId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();
        String body = finalFareBody(rideId, passengerId, "8.25");

        mockMvc.perform(post("/internal/v1/fares/final")
                        .header("X-Service-Token", SERVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(910.00));
        mockMvc.perform(post("/internal/v1/fares/final")
                        .header("X-Service-Token", SERVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mockMvc.perform(post("/internal/v1/fares/final")
                        .header("X-Service-Token", SERVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(finalFareBody(rideId, passengerId, "9.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FINAL_FARE_CONFLICT"));
    }

    @Test
    void successfulPaymentCreatesReceiptAndSupportsSafeReplay() throws Exception {
        UUID rideId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();
        when(rideServiceClient.getRideSummary(rideId)).thenReturn(completedRide(rideId, passengerId));
        UUID idempotencyKey = UUID.randomUUID();
        String body = paymentBody(rideId, "SUCCESS");

        MvcResult created = mockMvc.perform(post("/api/v1/payments")
                        .with(userJwt(passengerId, "PASSENGER"))
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.amount").value(1090.00))
                .andReturn();
        UUID paymentId = UUID.fromString(json(created, "paymentId"));

        mockMvc.perform(post("/api/v1/payments")
                        .with(userJwt(passengerId, "PASSENGER"))
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()));
        mockMvc.perform(get("/api/v1/payments/{id}/receipt", paymentId)
                        .with(userJwt(passengerId, "PASSENGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.notice")
                        .value("Simulated payment - no real money was transferred"));
    }

    @Test
    void failedPaymentAllowsLaterSuccessfulRetry() throws Exception {
        UUID rideId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();
        when(rideServiceClient.getRideSummary(rideId)).thenReturn(completedRide(rideId, passengerId));

        MvcResult failed = mockMvc.perform(post("/api/v1/payments")
                        .with(userJwt(passengerId, "PASSENGER"))
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(paymentBody(rideId, "FAILURE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andReturn();
        UUID failedPaymentId = UUID.fromString(json(failed, "paymentId"));
        mockMvc.perform(get("/api/v1/payments/{id}/receipt", failedPaymentId)
                        .with(userJwt(passengerId, "PASSENGER")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("RECEIPT_NOT_AVAILABLE"));

        mockMvc.perform(post("/api/v1/payments")
                        .with(userJwt(passengerId, "PASSENGER"))
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(paymentBody(rideId, "SUCCESS")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAID"));
        assertThat(paymentRepository.findAll()).hasSize(2);
    }

    @Test
    void paymentChecksRideStateAndPassengerOwnership() throws Exception {
        UUID rideId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();
        when(rideServiceClient.getRideSummary(rideId)).thenReturn(new RideSummaryResponse(
                rideId, passengerId, UUID.randomUUID(), "IN_PROGRESS",
                new BigDecimal("10.50"), null));

        mockMvc.perform(post("/api/v1/payments")
                        .with(userJwt(passengerId, "PASSENGER"))
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(paymentBody(rideId, "SUCCESS")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("RIDE_NOT_COMPLETED"));
        mockMvc.perform(post("/api/v1/payments")
                        .with(userJwt(UUID.randomUUID(), "PASSENGER"))
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(paymentBody(rideId, "SUCCESS")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void securityAndMalformedRequestsReturnStableErrors() throws Exception {
        mockMvc.perform(get("/internal/v1/fares/estimates/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_SERVICE_TOKEN"));
        mockMvc.perform(post("/api/v1/fares/estimates")
                        .with(userJwt(UUID.randomUUID(), "PASSENGER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
        mockMvc.perform(post("/api/v1/payments")
                        .with(userJwt(UUID.randomUUID(), "PASSENGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody(UUID.randomUUID(), "SUCCESS")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_REQUIRED_HEADER"));
    }

    private RideSummaryResponse completedRide(UUID rideId, UUID passengerId) {
        return new RideSummaryResponse(rideId, passengerId, UUID.randomUUID(), "COMPLETED",
                new BigDecimal("10.50"), new BigDecimal("1090.00"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor userJwt(
            UUID accountId, String role) {
        return jwt().jwt(token -> token.subject(accountId.toString()).claim("role", role))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private String finalFareBody(UUID rideId, UUID passengerId, String distance) {
        return """
                {"rideId":"%s","passengerId":"%s","distanceKm":%s}
                """.formatted(rideId, passengerId, distance);
    }

    private String paymentBody(UUID rideId, String outcome) {
        return """
                {"rideId":"%s","simulationOutcome":"%s","methodLabel":"SIMULATED_CARD"}
                """.formatted(rideId, outcome);
    }

    private String json(MvcResult result, String field) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray()).get(field).asText();
    }
}
