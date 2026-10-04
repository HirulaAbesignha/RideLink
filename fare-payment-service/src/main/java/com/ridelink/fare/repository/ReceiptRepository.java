package com.ridelink.fare.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ridelink.fare.entity.Receipt;

public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {

    Optional<Receipt> findByPaymentId(UUID paymentId);
}