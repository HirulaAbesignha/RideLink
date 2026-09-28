package com.ridelink.driver.repository;

import com.ridelink.driver.domain.DriverProfile;
import com.ridelink.driver.domain.DriverAvailability;
import com.ridelink.driver.domain.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverProfileRepository extends JpaRepository<DriverProfile, UUID> {
    Optional<DriverProfile> findByAccountId(UUID accountId);

    boolean existsByAccountId(UUID accountId);

    @Query("""
            select d from DriverProfile d, Vehicle v
            where v.driverId = d.id
              and d.availability = :availability
              and upper(d.serviceArea) = upper(:serviceArea)
              and v.status = :vehicleStatus
              and v.seatCapacity >= :seatCount
            order by d.id
            """)
    List<DriverProfile> findEligible(
            @Param("serviceArea") String serviceArea,
            @Param("seatCount") int seatCount,
            @Param("availability") DriverAvailability availability,
            @Param("vehicleStatus") VehicleStatus vehicleStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DriverProfile d
               set d.availability = :reserved,
                   d.reservedRideId = :rideId,
                   d.reservedAt = :reservedAt,
                   d.updatedAt = :reservedAt,
                   d.version = d.version + 1
             where d.id = :driverId
               and d.availability = :available
            """)
    int reserveIfAvailable(
            @Param("driverId") UUID driverId,
            @Param("rideId") UUID rideId,
            @Param("reservedAt") Instant reservedAt,
            @Param("available") DriverAvailability available,
            @Param("reserved") DriverAvailability reserved);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DriverProfile d
               set d.availability = :available,
                   d.reservedRideId = null,
                   d.reservedAt = null,
                   d.updatedAt = :updatedAt,
                   d.version = d.version + 1
             where d.id = :driverId
               and d.availability = :reserved
               and d.reservedRideId = :rideId
            """)
    int releaseMatchingReservation(
            @Param("driverId") UUID driverId,
            @Param("rideId") UUID rideId,
            @Param("updatedAt") Instant updatedAt,
            @Param("available") DriverAvailability available,
            @Param("reserved") DriverAvailability reserved);
}
