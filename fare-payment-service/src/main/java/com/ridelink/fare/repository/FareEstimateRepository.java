package com.ridelink.fare.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ridelink.fare.entity.FareEstimate;

public interface FareEstimateRepository extends JpaRepository<FareEstimate, UUID> {
}