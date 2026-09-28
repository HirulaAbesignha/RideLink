package com.ridelink.driver.service;

import com.ridelink.driver.api.dto.CreateDriverProfileRequest;
import com.ridelink.driver.api.dto.DriverProfileResponse;
import com.ridelink.driver.api.dto.UpdateAvailabilityRequest;
import com.ridelink.driver.api.dto.UpdateLocationRequest;
import com.ridelink.driver.api.dto.UpsertVehicleRequest;
import com.ridelink.driver.api.dto.VehicleResponse;
import com.ridelink.driver.api.error.ApiException;
import com.ridelink.driver.client.AccountClient;
import com.ridelink.driver.client.AccountSummary;
import com.ridelink.driver.domain.DriverAvailability;
import com.ridelink.driver.domain.DriverProfile;
import com.ridelink.driver.domain.Vehicle;
import com.ridelink.driver.domain.VehicleStatus;
import com.ridelink.driver.repository.DriverProfileRepository;
import com.ridelink.driver.repository.VehicleRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class DriverProfileService {

    private final DriverProfileRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final AccountClient accountClient;

    public DriverProfileService(DriverProfileRepository driverRepository,
                                VehicleRepository vehicleRepository,
                                AccountClient accountClient) {
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.accountClient = accountClient;
    }

    @Transactional
    public DriverProfileResponse createProfile(UUID accountId, CreateDriverProfileRequest request,
                                               String correlationId) {
        if (driverRepository.existsByAccountId(accountId)) {
            throw conflict("DRIVER_PROFILE_EXISTS", "A driver profile already exists for this account");
        }

        AccountSummary account = accountClient.getAccountSummary(accountId, correlationId);
        if (!accountId.equals(account.accountId())
                || !"DRIVER".equals(account.role())
                || !"ACTIVE".equals(account.status())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "ACCOUNT_NOT_ACTIVE_DRIVER", "The account is not an active driver");
        }

        DriverProfile profile = new DriverProfile(
                accountId,
                normalizeArea(request.serviceArea()),
                clean(request.locationName()),
                request.latitude(),
                request.longitude());
        try {
            return toResponse(driverRepository.save(profile), null);
        } catch (DataIntegrityViolationException exception) {
            throw conflict("DRIVER_PROFILE_EXISTS", "A driver profile already exists for this account");
        }
    }

    @Transactional(readOnly = true)
    public DriverProfileResponse getProfile(UUID accountId) {
        DriverProfile profile = findByAccountId(accountId);
        Vehicle vehicle = vehicleRepository.findByDriverId(profile.getId()).orElse(null);
        return toResponse(profile, vehicle);
    }

    @Transactional
    public VehicleSaveResult saveVehicle(UUID accountId, UpsertVehicleRequest request) {
        DriverProfile profile = findByAccountId(accountId);
        if (profile.getAvailability() == DriverAvailability.RESERVED) {
            throw conflict("DRIVER_RESERVED", "A reserved driver cannot replace the vehicle");
        }

        String registration = request.registrationNumber().strip().toUpperCase(Locale.ROOT);
        if (vehicleRepository.existsByRegistrationNumberAndDriverIdNot(registration, profile.getId())) {
            throw conflict("REGISTRATION_NUMBER_EXISTS", "The registration number is already in use");
        }

        Vehicle vehicle = vehicleRepository.findByDriverId(profile.getId()).orElse(null);
        boolean created = vehicle == null;
        if (created) {
            vehicle = new Vehicle(profile.getId(), registration, request.vehicleType(),
                    clean(request.manufacturer()), clean(request.model()), clean(request.colour()),
                    request.seatCapacity(), request.status());
        } else {
            vehicle.update(registration, request.vehicleType(), clean(request.manufacturer()),
                    clean(request.model()), clean(request.colour()), request.seatCapacity(), request.status());
        }

        try {
            vehicleRepository.save(vehicle);
        } catch (DataIntegrityViolationException exception) {
            throw conflict("REGISTRATION_NUMBER_EXISTS", "The registration number is already in use");
        }
        return new VehicleSaveResult(toResponse(profile, vehicle), created);
    }

    @Transactional
    public DriverProfileResponse updateAvailability(UUID accountId, UpdateAvailabilityRequest request) {
        DriverProfile profile = findByAccountId(accountId);
        if (profile.getAvailability() == DriverAvailability.RESERVED) {
            throw conflict("DRIVER_RESERVED", "A reserved driver cannot change availability");
        }
        if (request.availability() == DriverAvailability.RESERVED) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_FAILED",
                    "Drivers may request only AVAILABLE or OFFLINE");
        }
        if (request.availability() == DriverAvailability.AVAILABLE) {
            Vehicle vehicle = vehicleRepository.findByDriverId(profile.getId()).orElse(null);
            if (vehicle == null || vehicle.getStatus() != VehicleStatus.ACTIVE) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "ACTIVE_VEHICLE_REQUIRED", "An active vehicle is required");
            }
            if (profile.getLocationName().isBlank() || profile.getServiceArea().isBlank()) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "LOCATION_REQUIRED", "A current location is required");
            }
        }
        profile.changeAvailability(request.availability());
        return toResponse(profile, vehicleRepository.findByDriverId(profile.getId()).orElse(null));
    }

    @Transactional
    public DriverProfileResponse updateLocation(UUID accountId, UpdateLocationRequest request) {
        DriverProfile profile = findByAccountId(accountId);
        profile.updateLocation(normalizeArea(request.serviceArea()), clean(request.locationName()),
                request.latitude(), request.longitude());
        return toResponse(profile, vehicleRepository.findByDriverId(profile.getId()).orElse(null));
    }

    private DriverProfile findByAccountId(UUID accountId) {
        return driverRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "DRIVER_PROFILE_NOT_FOUND", "Driver profile was not found"));
    }

    private DriverProfileResponse toResponse(DriverProfile profile, Vehicle vehicle) {
        VehicleResponse vehicleResponse = vehicle == null ? null : new VehicleResponse(
                vehicle.getId(), vehicle.getRegistrationNumber(), vehicle.getVehicleType(),
                vehicle.getManufacturer(), vehicle.getModel(), vehicle.getColour(),
                vehicle.getSeatCapacity(), vehicle.getStatus());
        return new DriverProfileResponse(profile.getId(), profile.getAccountId(),
                profile.getServiceArea(), profile.getLocationName(), profile.getLatitude(),
                profile.getLongitude(), profile.getAvailability(), vehicleResponse);
    }

    private String normalizeArea(String value) {
        return clean(value).toUpperCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value.strip().replaceAll("\\s+", " ");
    }

    private ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }
}
