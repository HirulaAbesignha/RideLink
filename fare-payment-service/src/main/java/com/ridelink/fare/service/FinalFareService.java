package com.ridelink.fare.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridelink.fare.dto.FinalFareRequest;
import com.ridelink.fare.dto.FinalFareResponse;
import com.ridelink.fare.dto.FinalFareCreationResult;
import com.ridelink.fare.entity.FinalFare;
import com.ridelink.fare.exception.FareApiException;
import com.ridelink.fare.repository.FinalFareRepository;

@Service
public class FinalFareService {

    private static final BigDecimal BASE_FARE = new BigDecimal("250.00");
    private static final BigDecimal DISTANCE_RATE = new BigDecimal("80.00");
    private static final String CURRENCY = "LKR";
    private static final String RULE_VERSION = "RIDELINK_2026_V1";

    private final FinalFareRepository finalFareRepository;

    public FinalFareService(FinalFareRepository finalFareRepository) {
        this.finalFareRepository = finalFareRepository;
    }

    @Transactional
    public FinalFareCreationResult createFinalFare(FinalFareRequest request) {

        var existing = finalFareRepository.findByRideId(request.rideId());
        if (existing.isPresent()) {
            FinalFare savedFare = existing.get();
            if (savedFare.getPassengerId().equals(request.passengerId())
                    && savedFare.getDistanceKm().compareTo(request.distanceKm()) == 0) {
                return new FinalFareCreationResult(toResponse(savedFare), true);
            }
            throw new FareApiException(409, "FINAL_FARE_CONFLICT",
                    "A different final fare already exists for this ride");
        }

        BigDecimal amount = BASE_FARE
                .add(request.distanceKm().multiply(DISTANCE_RATE))
                .setScale(2, RoundingMode.HALF_UP);

        FinalFare finalFare = new FinalFare();

        finalFare.setFareId(UUID.randomUUID());
        finalFare.setRideId(request.rideId());
        finalFare.setPassengerId(request.passengerId());
        finalFare.setDistanceKm(request.distanceKm());
        finalFare.setAmount(amount);
        finalFare.setCurrency(CURRENCY);
        finalFare.setRuleVersion(RULE_VERSION);
        finalFare.setCreatedAt(Instant.now());

        FinalFare savedFare = finalFareRepository.save(finalFare);

        return new FinalFareCreationResult(toResponse(savedFare), false);
    }

    private FinalFareResponse toResponse(FinalFare savedFare) {
        return new FinalFareResponse(
                savedFare.getFareId(),
                savedFare.getRideId(),
                savedFare.getPassengerId(),
                savedFare.getDistanceKm(),
                savedFare.getAmount(),
                savedFare.getCurrency(),
                savedFare.getRuleVersion(),
                savedFare.getCreatedAt());
    }
}
