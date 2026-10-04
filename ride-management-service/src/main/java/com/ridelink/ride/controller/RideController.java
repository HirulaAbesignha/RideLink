package com.ridelink.ride.controller;

import com.ridelink.ride.entity.Ride;
import com.ridelink.ride.service.RideService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rides")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Ride createRide(@RequestBody Ride ride) {
        return rideService.createRide(ride);
    }

    @GetMapping("/{id}")
    public Ride getRide(@PathVariable UUID id) {
        return rideService.getRide(id);
    }

    @PostMapping("/{id}/accept")
    public Ride acceptRide(@PathVariable UUID id) {
        return rideService.acceptRide(id);
    }

    @PostMapping("/{id}/start")
    public Ride startRide(@PathVariable UUID id) {
        return rideService.startRide(id);
    }
}