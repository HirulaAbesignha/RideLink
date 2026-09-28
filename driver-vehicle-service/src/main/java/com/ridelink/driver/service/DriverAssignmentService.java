package com.ridelink.driver.service;

import com.ridelink.driver.api.dto.EligibleDriverResponse;
import com.ridelink.driver.api.dto.EligibleDriversResponse;
import com.ridelink.driver.api.dto.ReservationResponse;
import com.ridelink.driver.api.error.ApiException;
import com.ridelink.driver.domain.DriverAvailability;
import com.ridelink.driver.domain.DriverProfile;
import com.ridelink.driver.domain.Vehicle;
import com.ridelink.driver.domain.VehicleStatus;
import com.ridelink.driver.repository.DriverProfileRepository;
import com.ridelink.driver.repository.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DriverAssignmentService {

    private final DriverProfileRepository driverRepository;
    private final VehicleRepository vehicleRepository;

    public DriverAssignmentService(DriverProfileRepository driverRepository,
                                   VehicleRepository vehicleRepository) {
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional(readOnly = true)
    public EligibleDriversResponse findEligible(String serviceArea, int seatCount) {
        String normalizedArea = serviceArea.strip().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        List<DriverProfile> drivers = driverRepository.findEligible(
                normalizedArea, seatCount, DriverAvailability.AVAILABLE, VehicleStatus.ACTIVE);

        List<UUID> driverIds = drivers.stream().map(DriverProfile::getId).toList();
        Map<UUID, Vehicle> vehicles = driverIds.isEmpty()
                ? Map.of()
                : vehicleRepository.findAllByDriverIdIn(driverIds).stream()
                    .collect(Collectors.toMap(Vehicle::getDriverId, Function.identity()));

        List<EligibleDriverResponse> responses = drivers.stream()
                .map(driver -> toEligibleResponse(driver, vehicles.get(driver.getId())))
                .toList();
        return new EligibleDriversResponse(responses);
    }

    @Transactional
    public ReservationSaveResult reserve(UUID driverId, UUID rideId) {
        Instant reservedAt = Instant.now();
        int changed = driverRepository.reserveIfAvailable(
                driverId, rideId, reservedAt,
                DriverAvailability.AVAILABLE, DriverAvailability.RESERVED);
        if (changed == 1) {
            return new ReservationSaveResult(
                    new ReservationResponse(driverId, rideId, DriverAvailability.RESERVED, reservedAt), true);
        }

        DriverProfile driver = findDriver(driverId);
        if (driver.getAvailability() == DriverAvailability.RESERVED
                && rideId.equals(driver.getReservedRideId())) {
            return new ReservationSaveResult(toReservationResponse(driver), false);
        }
        throw new ApiException(HttpStatus.CONFLICT, "DRIVER_NOT_AVAILABLE",
                "The selected driver is not available");
    }

    @Transactional
    public void release(UUID driverId, UUID rideId) {
        DriverProfile driver = findDriver(driverId);
        if (driver.getAvailability() != DriverAvailability.RESERVED) {
            return;
        }
        if (!rideId.equals(driver.getReservedRideId())) {
            throw mismatch();
        }

        int changed = driverRepository.releaseMatchingReservation(
                driverId, rideId, Instant.now(),
                DriverAvailability.AVAILABLE, DriverAvailability.RESERVED);
        if (changed == 1) {
            return;
        }

        DriverProfile current = findDriver(driverId);
        if (current.getAvailability() == DriverAvailability.RESERVED
                && !rideId.equals(current.getReservedRideId())) {
            throw mismatch();
        }
    }

    private DriverProfile findDriver(UUID driverId) {
        return driverRepository.findById(driverId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "DRIVER_PROFILE_NOT_FOUND", "Driver profile was not found"));
    }

    private EligibleDriverResponse toEligibleResponse(DriverProfile driver, Vehicle vehicle) {
        return new EligibleDriverResponse(driver.getId(), driver.getAccountId(),
                driver.getServiceArea(), driver.getLocationName(),
                vehicle.getVehicleType(), vehicle.getSeatCapacity());
    }

    private ReservationResponse toReservationResponse(DriverProfile driver) {
        return new ReservationResponse(driver.getId(), driver.getReservedRideId(),
                driver.getAvailability(), driver.getReservedAt());
    }

    private ApiException mismatch() {
        return new ApiException(HttpStatus.CONFLICT, "RESERVATION_MISMATCH",
                "The ride does not match the driver's current reservation");
    }
}
