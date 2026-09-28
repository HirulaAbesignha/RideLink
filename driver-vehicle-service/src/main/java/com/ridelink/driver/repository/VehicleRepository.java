package com.ridelink.driver.repository;

import com.ridelink.driver.domain.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    Optional<Vehicle> findByDriverId(UUID driverId);

    boolean existsByRegistrationNumberAndDriverIdNot(String registrationNumber, UUID driverId);

    List<Vehicle> findAllByDriverIdIn(Iterable<UUID> driverIds);
}
