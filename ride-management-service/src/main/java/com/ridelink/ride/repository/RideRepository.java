package com.ridelink.ride.repository;

import com.ridelink.ride.entity.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RideRepository extends JpaRepository<Ride, UUID> {
}