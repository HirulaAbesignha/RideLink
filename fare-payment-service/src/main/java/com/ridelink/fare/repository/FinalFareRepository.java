package com.ridelink.fare.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ridelink.fare.entity.FinalFare;

public interface FinalFareRepository extends JpaRepository<FinalFare, UUID> {

    Optional<FinalFare> findByRideId(UUID rideId);
}