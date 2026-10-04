package com.ridelink.fare.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ridelink.fare.dto.CreateEstimateRequest;
import com.ridelink.fare.dto.EstimateResponse;
import com.ridelink.fare.entity.FareEstimate;
import com.ridelink.fare.exception.FareApiException;
import com.ridelink.fare.repository.FareEstimateRepository;

@Service
public class FareService {

    private static final BigDecimal BASE_FARE = new BigDecimal("250.00");
    private static final BigDecimal DISTANCE_RATE = new BigDecimal("80.00");
    private static final String CURRENCY = "LKR";
    private static final String RULE_VERSION = "RIDELINK_2026_V1";

    private final FareEstimateRepository fareEstimateRepository;

    public FareService(FareEstimateRepository fareEstimateRepository) {
        this.fareEstimateRepository = fareEstimateRepository;
    }

    public EstimateResponse createEstimate(CreateEstimateRequest request) {

        BigDecimal amount = BASE_FARE
                .add(request.distanceKm().multiply(DISTANCE_RATE))
                .setScale(2, RoundingMode.HALF_UP);

        Instant createdAt = Instant.now();
        Instant expiresAt = createdAt.plus(30, ChronoUnit.MINUTES);

        FareEstimate estimate = new FareEstimate();

        estimate.setEstimateId(UUID.randomUUID());
        estimate.setPickup(request.pickup().trim());
        estimate.setDestination(request.destination().trim());
        estimate.setDistanceKm(request.distanceKm());
        estimate.setAmount(amount);
        estimate.setCurrency(CURRENCY);
        estimate.setRuleVersion(RULE_VERSION);
        estimate.setCreatedAt(createdAt);
        estimate.setExpiresAt(expiresAt);

        FareEstimate savedEstimate = fareEstimateRepository.save(estimate);

        return toEstimateResponse(savedEstimate);
    }

    public EstimateResponse getEstimate(UUID estimateId) {

        FareEstimate estimate = fareEstimateRepository.findById(estimateId)
                .orElseThrow(() -> new FareApiException(404,
                        "FARE_ESTIMATE_NOT_FOUND", "Fare estimate not found"));

        if (Instant.now().isAfter(estimate.getExpiresAt())) {
            throw new FareApiException(422, "FARE_ESTIMATE_EXPIRED", "Fare estimate has expired");
        }

        return toEstimateResponse(estimate);
    }

    private EstimateResponse toEstimateResponse(FareEstimate estimate) {

        return new EstimateResponse(
                estimate.getEstimateId(),
                estimate.getPickup(),
                estimate.getDestination(),
                estimate.getDistanceKm(),
                estimate.getAmount(),
                estimate.getCurrency(),
                estimate.getRuleVersion(),
                estimate.getCreatedAt(),
                estimate.getExpiresAt()
        );
    }
}
