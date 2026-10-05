package com.ridelink.fare.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ridelink.fare.dto.FinalFareRequest;
import com.ridelink.fare.dto.FinalFareResponse;
import com.ridelink.fare.service.FinalFareService;
import com.ridelink.fare.service.FareService;
import com.ridelink.fare.dto.EstimateResponse;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/internal/v1/fares")
@Tag(name = "Internal Fare Management", description = "Internal final fare operations")
public class InternalFareController {

    private final FinalFareService finalFareService;
    private final FareService fareService;

    public InternalFareController(FinalFareService finalFareService, FareService fareService) {
        this.finalFareService = finalFareService;
        this.fareService = fareService;
    }

    @GetMapping("/estimates/{estimateId}")
    @Operation(summary = "Validate and retrieve an estimate for Ride Service")
    public ResponseEntity<EstimateResponse> getEstimate(@PathVariable UUID estimateId) {
        return ResponseEntity.ok(fareService.getEstimate(estimateId));
    }

    @PostMapping("/final")
    @Operation(summary = "Create final fare")
    public ResponseEntity<FinalFareResponse> createFinalFare(
            @Valid @RequestBody FinalFareRequest request) {

        var result = finalFareService.createFinalFare(request);
        return ResponseEntity.status(result.replayed() ? 200 : 201).body(result.fare());
    }
}
