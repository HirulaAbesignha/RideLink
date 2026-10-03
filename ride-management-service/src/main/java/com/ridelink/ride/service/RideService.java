package com.ridelink.ride.service;

import com.ridelink.ride.entity.Ride;
import com.ridelink.ride.entity.RideStatus;
import com.ridelink.ride.repository.RideRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RideService {

    private final RideRepository rideRepository;

    public RideService(RideRepository rideRepository) {
        this.rideRepository = rideRepository;
    }

    public Ride createRide(Ride ride) {
        ride.setStatus(RideStatus.REQUESTED);
        return rideRepository.save(ride);
    }

    public Ride getRide(UUID id) {
        return rideRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ride not found: " + id));
    }

    public Ride acceptRide(UUID id) {
        Ride ride = getRide(id);

        if (ride.getStatus() != RideStatus.ASSIGNED) {
            throw new RuntimeException(
                    "Ride can only be accepted when status is ASSIGNED");
        }

        ride.setStatus(RideStatus.ACCEPTED);
        return rideRepository.save(ride);
    }

    public Ride startRide(UUID id) {
        Ride ride = getRide(id);

        if (ride.getStatus() != RideStatus.ACCEPTED) {
            throw new RuntimeException(
                    "Ride can only be started when status is ACCEPTED");
        }

        ride.setStatus(RideStatus.IN_PROGRESS);
        return rideRepository.save(ride);
    }
}