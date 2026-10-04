package com.ridelink.ride.controller;

import com.ridelink.ride.api.dto.CancelRideRequest;
import com.ridelink.ride.api.dto.CreateRideRequest;
import com.ridelink.ride.api.dto.RideCreationResult;
import com.ridelink.ride.api.dto.RidePageResponse;
import com.ridelink.ride.api.dto.RideResponse;
import com.ridelink.ride.api.error.ApiException;
import com.ridelink.ride.entity.RideStatus;
import com.ridelink.ride.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/rides")
@Tag(name = "Ride management")
@SecurityRequirement(name = "bearerAuth")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping
    @Operation(summary = "Create a ride and reserve an eligible driver")
    public ResponseEntity<RideResponse> createRide(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateRideRequest request) {
        RideCreationResult result = rideService.createRide(subject(jwt), idempotencyKey, request);
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED).body(result.ride());
    }

    @GetMapping("/{rideId}")
    @Operation(summary = "View a ride as its passenger, assigned driver or administrator")
    public RideResponse getRide(@PathVariable UUID rideId, @AuthenticationPrincipal Jwt jwt) {
        return rideService.getRide(rideId, subject(jwt), role(jwt));
    }

    @GetMapping
    @Operation(summary = "List rides visible to the signed in account")
    public RidePageResponse listRides(
            @RequestParam(required = false) RideStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @AuthenticationPrincipal Jwt jwt) {
        return RidePageResponse.from(rideService.listRides(subject(jwt), role(jwt), status, page, size));
    }

    @PostMapping("/{rideId}/accept")
    @Operation(summary = "Accept an assigned ride")
    public RideResponse accept(@PathVariable UUID rideId, @AuthenticationPrincipal Jwt jwt) {
        return rideService.accept(rideId, subject(jwt));
    }

    @PostMapping("/{rideId}/start")
    @Operation(summary = "Start an accepted ride")
    public RideResponse start(@PathVariable UUID rideId, @AuthenticationPrincipal Jwt jwt) {
        return rideService.start(rideId, subject(jwt));
    }

    @PostMapping("/{rideId}/complete")
    @Operation(summary = "Complete a ride after final fare creation")
    public RideResponse complete(@PathVariable UUID rideId, @AuthenticationPrincipal Jwt jwt) {
        return rideService.complete(rideId, subject(jwt));
    }

    @PostMapping("/{rideId}/cancel")
    @Operation(summary = "Cancel a ride before it starts")
    public RideResponse cancel(@PathVariable UUID rideId, @AuthenticationPrincipal Jwt jwt,
                               @Valid @RequestBody CancelRideRequest request) {
        return rideService.cancel(rideId, subject(jwt), request);
    }

    private UUID subject(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "The token subject is invalid");
        }
    }

    private String role(Jwt jwt) {
        return jwt.getClaimAsString("role");
    }
}
