package com.ridelink.driver.controller;

import com.ridelink.driver.api.dto.CreateDriverProfileRequest;
import com.ridelink.driver.api.dto.DriverProfileResponse;
import com.ridelink.driver.api.dto.UpdateAvailabilityRequest;
import com.ridelink.driver.api.dto.UpdateLocationRequest;
import com.ridelink.driver.api.dto.UpsertVehicleRequest;
import com.ridelink.driver.config.CorrelationIdFilter;
import com.ridelink.driver.service.DriverProfileService;
import com.ridelink.driver.service.VehicleSaveResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/drivers")
@Tag(name = "Driver profile")
public class DriverController {

    private final DriverProfileService service;

    public DriverController(DriverProfileService service) {
        this.service = service;
    }

    @PostMapping("/me/profile")
    @Operation(summary = "Create the signed-in driver's operational profile")
    ResponseEntity<DriverProfileResponse> createProfile(@AuthenticationPrincipal Jwt jwt,
                                                        @Valid @RequestBody CreateDriverProfileRequest request,
                                                        HttpServletRequest servletRequest) {
        String correlationId = servletRequest.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME).toString();
        DriverProfileResponse response = service.createProfile(subject(jwt), request, correlationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    @Operation(summary = "View the signed-in driver's profile and vehicle")
    DriverProfileResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        return service.getProfile(subject(jwt));
    }

    @PutMapping("/me/vehicle")
    @Operation(summary = "Create or replace the signed-in driver's vehicle")
    ResponseEntity<DriverProfileResponse> saveVehicle(@AuthenticationPrincipal Jwt jwt,
                                                      @Valid @RequestBody UpsertVehicleRequest request) {
        VehicleSaveResult result = service.saveVehicle(subject(jwt), request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.profile());
    }

    @PatchMapping("/me/availability")
    @Operation(summary = "Change availability to AVAILABLE or OFFLINE")
    DriverProfileResponse updateAvailability(@AuthenticationPrincipal Jwt jwt,
                                             @Valid @RequestBody UpdateAvailabilityRequest request) {
        return service.updateAvailability(subject(jwt), request);
    }

    @PatchMapping("/me/location")
    @Operation(summary = "Update the driver's simulated location")
    DriverProfileResponse updateLocation(@AuthenticationPrincipal Jwt jwt,
                                         @Valid @RequestBody UpdateLocationRequest request) {
        return service.updateLocation(subject(jwt), request);
    }

    private UUID subject(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
