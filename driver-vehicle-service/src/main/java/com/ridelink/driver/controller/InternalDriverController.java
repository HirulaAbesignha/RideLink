package com.ridelink.driver.controller;

import com.ridelink.driver.api.dto.EligibleDriversResponse;
import com.ridelink.driver.api.dto.ReservationRequest;
import com.ridelink.driver.api.dto.ReservationResponse;
import com.ridelink.driver.service.DriverAssignmentService;
import com.ridelink.driver.service.ReservationSaveResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/internal/v1/drivers")
@Tag(name = "Driver assignment")
@SecurityRequirement(name = "serviceToken")
public class InternalDriverController {

    private final DriverAssignmentService service;

    public InternalDriverController(DriverAssignmentService service) {
        this.service = service;
    }

    @GetMapping("/eligible")
    @Operation(summary = "Find available drivers for Ride Service")
    EligibleDriversResponse findEligible(
            @RequestParam @NotBlank @Size(max = 80) String serviceArea,
            @RequestParam @Min(1) @Max(12) int seatCount) {
        return service.findEligible(serviceArea, seatCount);
    }

    @PostMapping("/{driverId}/reservations")
    @Operation(summary = "Atomically reserve an available driver")
    ResponseEntity<ReservationResponse> reserve(
            @PathVariable UUID driverId,
            @Valid @RequestBody ReservationRequest request) {
        ReservationSaveResult result = service.reserve(driverId, request.rideId());
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.reservation());
    }

    @DeleteMapping("/{driverId}/reservations/{rideId}")
    @Operation(summary = "Release a driver's matching reservation")
    ResponseEntity<Void> release(@PathVariable UUID driverId, @PathVariable UUID rideId) {
        service.release(driverId, rideId);
        return ResponseEntity.noContent().build();
    }
}
