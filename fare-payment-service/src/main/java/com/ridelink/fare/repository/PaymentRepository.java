package com.ridelink.fare.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ridelink.fare.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findFirstByRideIdAndStatusOrderByRecordedAtDesc(UUID rideId, String status);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
