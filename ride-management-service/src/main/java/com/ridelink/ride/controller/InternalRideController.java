package com.ridelink.ride.controller;

import com.ridelink.ride.api.dto.RideSummaryResponse;
import com.ridelink.ride.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/rides")
@Tag(name = "Internal ride access")
@SecurityRequirement(name = "serviceToken")
public class InternalRideController {

    private final RideService rideService;

    public InternalRideController(RideService rideService) {
        this.rideService = rideService;
    }

    @GetMapping("/{rideId}/summary")
    @Operation(summary = "Return the payment authorization summary for a ride")
    public RideSummaryResponse getSummary(@PathVariable UUID rideId) {
        return rideService.getSummary(rideId);
    }
}
