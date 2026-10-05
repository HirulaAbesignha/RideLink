package com.ridelink.ride.repository;

import com.ridelink.ride.entity.Ride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface RideRepository extends JpaRepository<Ride, UUID> {
    Optional<Ride> findByIdempotencyKey(UUID idempotencyKey);
    Page<Ride> findByPassengerId(UUID passengerId, Pageable pageable);
    Page<Ride> findByPassengerIdAndStatus(UUID passengerId, com.ridelink.ride.entity.RideStatus status,
                                          Pageable pageable);
    Page<Ride> findByDriverAccountId(UUID driverAccountId, Pageable pageable);
    Page<Ride> findByDriverAccountIdAndStatus(UUID driverAccountId, com.ridelink.ride.entity.RideStatus status,
                                              Pageable pageable);
    Page<Ride> findByStatus(com.ridelink.ride.entity.RideStatus status, Pageable pageable);
}
