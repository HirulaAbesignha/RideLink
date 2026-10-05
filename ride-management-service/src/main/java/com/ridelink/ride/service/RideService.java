package com.ridelink.ride.service;

import com.ridelink.ride.api.dto.CancelRideRequest;
import com.ridelink.ride.api.dto.CreateRideRequest;
import com.ridelink.ride.api.dto.RideCreationResult;
import com.ridelink.ride.api.dto.RideResponse;
import com.ridelink.ride.api.dto.RideSummaryResponse;
import com.ridelink.ride.api.error.ApiException;
import com.ridelink.ride.client.DriverServiceClient;
import com.ridelink.ride.client.EligibleDriver;
import com.ridelink.ride.client.FareEstimateResponse;
import com.ridelink.ride.client.FareServiceClient;
import com.ridelink.ride.client.FinalFareResponse;
import com.ridelink.ride.entity.Ride;
import com.ridelink.ride.entity.RideStatus;
import com.ridelink.ride.repository.RideRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

@Service
public class RideService {

    private static final Logger log = LoggerFactory.getLogger(RideService.class);

    private final RideRepository rideRepository;
    private final DriverServiceClient driverClient;
    private final FareServiceClient fareClient;

    public RideService(RideRepository rideRepository,
                       DriverServiceClient driverClient,
                       FareServiceClient fareClient) {
        this.rideRepository = rideRepository;
        this.driverClient = driverClient;
        this.fareClient = fareClient;
    }

    public RideCreationResult createRide(UUID passengerId, UUID idempotencyKey,
                                         CreateRideRequest request) {
        Ride existing = rideRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            if (!matches(existing, passengerId, request)) {
                throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT",
                        "This idempotency key was used with a different request");
            }
            return new RideCreationResult(toResponse(existing), true);
        }

        FareEstimateResponse estimate = fareClient.getEstimate(request.fareEstimateId());
        validateEstimate(estimate, request);
        Ride ride = new Ride(
                passengerId,
                request.pickup().strip(),
                request.destination().strip(),
                request.serviceArea().strip().toUpperCase(Locale.ROOT),
                request.distanceKm(),
                request.seatCount(),
                request.fareEstimateId(),
                estimate.amount(),
                estimate.currency(),
                idempotencyKey);
        rideRepository.saveAndFlush(ride);

        for (EligibleDriver driver : driverClient.findEligible(ride.getServiceArea(), ride.getSeatCount())) {
            if (!driverClient.reserve(driver.driverId(), ride.getId())) {
                continue;
            }
            try {
                ride.assign(driver.driverId(), driver.accountId());
                return new RideCreationResult(toResponse(rideRepository.saveAndFlush(ride)), false);
            } catch (RuntimeException exception) {
                driverClient.release(driver.driverId(), ride.getId());
                throw exception;
            }
        }

        throw new ApiException(HttpStatus.CONFLICT, "NO_AVAILABLE_DRIVER",
                "No eligible driver could be reserved");
    }

    @Transactional(readOnly = true)
    public RideResponse getRide(UUID rideId, UUID callerId, String role) {
        Ride ride = requireRide(rideId);
        requireParticipantOrAdmin(ride, callerId, role);
        return toResponse(ride);
    }

    @Transactional(readOnly = true)
    public Page<RideResponse> listRides(UUID callerId, String role, RideStatus status,
                                        int page, int size) {
        PageRequest request = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Ride> rides;
        if ("ADMIN".equals(role)) {
            rides = status == null ? rideRepository.findAll(request) : rideRepository.findByStatus(status, request);
        } else if ("PASSENGER".equals(role)) {
            rides = status == null
                    ? rideRepository.findByPassengerId(callerId, request)
                    : rideRepository.findByPassengerIdAndStatus(callerId, status, request);
        } else if ("DRIVER".equals(role)) {
            rides = status == null
                    ? rideRepository.findByDriverAccountId(callerId, request)
                    : rideRepository.findByDriverAccountIdAndStatus(callerId, status, request);
        } else {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "This role cannot view rides");
        }
        return rides.map(this::toResponse);
    }

    @Transactional
    public RideResponse accept(UUID rideId, UUID driverAccountId) {
        Ride ride = requireAssignedDriver(rideId, driverAccountId);
        requireStatus(ride, RideStatus.ASSIGNED);
        ride.accept();
        return toResponse(ride);
    }

    @Transactional
    public RideResponse start(UUID rideId, UUID driverAccountId) {
        Ride ride = requireAssignedDriver(rideId, driverAccountId);
        requireStatus(ride, RideStatus.ACCEPTED);
        ride.start();
        return toResponse(ride);
    }

    @Transactional
    public RideResponse complete(UUID rideId, UUID driverAccountId) {
        Ride ride = requireAssignedDriver(rideId, driverAccountId);
        requireStatus(ride, RideStatus.IN_PROGRESS);
        FinalFareResponse fare = fareClient.createFinalFare(
                ride.getId(), ride.getPassengerId(), ride.getDistanceKm());
        if (fare == null || fare.amount() == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "FARE_SERVICE_UNAVAILABLE",
                    "Fare Service returned no final fare");
        }
        ride.complete(fare.amount());
        rideRepository.saveAndFlush(ride);
        try {
            driverClient.release(ride.getDriverId(), ride.getId());
        } catch (ApiException exception) {
            log.warn("Driver release will need a retry for ride {}", ride.getId());
        }
        return toResponse(ride);
    }

    @Transactional
    public RideResponse cancel(UUID rideId, UUID passengerId, CancelRideRequest request) {
        Ride ride = requireRide(rideId);
        if (!ride.getPassengerId().equals(passengerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_RIDE_PASSENGER",
                    "Only the ride passenger may cancel this ride");
        }
        if (ride.getStatus() != RideStatus.REQUESTED
                && ride.getStatus() != RideStatus.ASSIGNED
                && ride.getStatus() != RideStatus.ACCEPTED) {
            invalidTransition(ride);
        }
        if (ride.getDriverId() != null) {
            driverClient.release(ride.getDriverId(), ride.getId());
        }
        ride.cancel(request.reason().strip());
        return toResponse(ride);
    }

    @Transactional(readOnly = true)
    public RideSummaryResponse getSummary(UUID rideId) {
        Ride ride = requireRide(rideId);
        return new RideSummaryResponse(
                ride.getId(), ride.getPassengerId(), ride.getDriverAccountId(), ride.getStatus(),
                ride.getDistanceKm(), ride.getFinalFare());
    }

    private Ride requireAssignedDriver(UUID rideId, UUID driverAccountId) {
        Ride ride = requireRide(rideId);
        if (ride.getDriverAccountId() == null || !ride.getDriverAccountId().equals(driverAccountId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_ASSIGNED_DRIVER",
                    "Only the assigned driver may change this ride");
        }
        return ride;
    }

    private void requireParticipantOrAdmin(Ride ride, UUID callerId, String role) {
        if ("ADMIN".equals(role)
                || ride.getPassengerId().equals(callerId)
                || callerId.equals(ride.getDriverAccountId())) {
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "Only a ride participant or administrator may view this ride");
    }

    private Ride requireRide(UUID rideId) {
        return rideRepository.findById(rideId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RIDE_NOT_FOUND", "Ride not found"));
    }

    private void requireStatus(Ride ride, RideStatus expected) {
        if (ride.getStatus() != expected) {
            invalidTransition(ride);
        }
    }

    private void invalidTransition(Ride ride) {
        throw new ApiException(HttpStatus.CONFLICT, "INVALID_RIDE_TRANSITION",
                "The ride cannot change from its current status");
    }

    private void validateEstimate(FareEstimateResponse estimate, CreateRideRequest request) {
        if (estimate == null
                || !request.fareEstimateId().equals(estimate.estimateId())
                || !request.pickup().strip().equalsIgnoreCase(estimate.pickup().strip())
                || !request.destination().strip().equalsIgnoreCase(estimate.destination().strip())
                || request.distanceKm().compareTo(estimate.distanceKm()) != 0) {
            throw new ApiException(HttpStatus.CONFLICT, "FARE_ESTIMATE_MISMATCH",
                    "The fare estimate does not match this trip");
        }
    }

    private boolean matches(Ride ride, UUID passengerId, CreateRideRequest request) {
        return ride.getPassengerId().equals(passengerId)
                && ride.getPickup().equals(request.pickup().strip())
                && ride.getDestination().equals(request.destination().strip())
                && ride.getServiceArea().equals(request.serviceArea().strip().toUpperCase(Locale.ROOT))
                && ride.getDistanceKm().compareTo(request.distanceKm()) == 0
                && ride.getSeatCount() == request.seatCount()
                && ride.getFareEstimateId().equals(request.fareEstimateId());
    }

    private RideResponse toResponse(Ride ride) {
        return new RideResponse(
                ride.getId(), ride.getPassengerId(), ride.getDriverId(), ride.getDriverAccountId(),
                ride.getPickup(), ride.getDestination(), ride.getServiceArea(), ride.getDistanceKm(),
                ride.getSeatCount(), ride.getFareEstimateId(), ride.getEstimatedFare(), ride.getFinalFare(),
                ride.getCurrency(), ride.getStatus(), ride.getCancellationReason(), ride.getAcceptedAt(),
                ride.getStartedAt(), ride.getCompletedAt(), ride.getCancelledAt(),
                ride.getCreatedAt(), ride.getUpdatedAt());
    }
}
