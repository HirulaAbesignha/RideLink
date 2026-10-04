package com.ridelink.fare.controller;

import java.util.UUID;

import com.ridelink.fare.client.RideServiceClient;
import com.ridelink.fare.dto.FinalFareResponse;
import com.ridelink.fare.dto.RideSummaryResponse;
import com.ridelink.fare.entity.FinalFare;
import com.ridelink.fare.exception.FareApiException;
import com.ridelink.fare.repository.FinalFareRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fares/rides")
@Tag(name = "Fare Management", description = "Final fare retrieval")
public class FinalFareQueryController {
    private final FinalFareRepository repository;
    private final RideServiceClient rideServiceClient;

    public FinalFareQueryController(FinalFareRepository repository, RideServiceClient rideServiceClient) {
        this.repository = repository;
        this.rideServiceClient = rideServiceClient;
    }

    @GetMapping("/{rideId}")
    @Operation(summary = "Get the final fare for a ride participant or administrator")
    public ResponseEntity<FinalFareResponse> getFinalFare(@PathVariable UUID rideId,
            @AuthenticationPrincipal Jwt jwt) {
        RideSummaryResponse ride = rideServiceClient.getRideSummary(rideId);
        UUID callerId = UUID.fromString(jwt.getSubject());
        String role = jwt.getClaimAsString("role");
        boolean participant = callerId.equals(ride.passengerId()) || callerId.equals(ride.driverAccountId());
        if (!participant && !"ADMIN".equals(role)) {
            throw new FareApiException(403, "ACCESS_DENIED", "Only a ride participant or administrator may view the fare");
        }
        FinalFare fare = repository.findByRideId(rideId)
                .orElseThrow(() -> new FareApiException(404, "FINAL_FARE_NOT_FOUND", "Final fare not found"));
        return ResponseEntity.ok(new FinalFareResponse(fare.getFareId(), fare.getRideId(), fare.getPassengerId(),
                fare.getDistanceKm(), fare.getAmount(), fare.getCurrency(), fare.getRuleVersion(), fare.getCreatedAt()));
    }
}
