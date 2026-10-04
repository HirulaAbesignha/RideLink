package com.ridelink.fare.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ridelink.fare.dto.CreateEstimateRequest;
import com.ridelink.fare.dto.EstimateResponse;
import com.ridelink.fare.service.FareService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/fares")
@Tag(name = "Fare Management", description = "Fare estimate operations")
public class FareController {

    private final FareService fareService;

    public FareController(FareService fareService) {
        this.fareService = fareService;
    }

    @PostMapping("/estimates")
    @Operation(summary = "Create a fare estimate")
    public ResponseEntity<EstimateResponse> createEstimate(
            @Valid @RequestBody CreateEstimateRequest request) {

        EstimateResponse response = fareService.createEstimate(request);

        return ResponseEntity.status(201).body(response);
    }

}
