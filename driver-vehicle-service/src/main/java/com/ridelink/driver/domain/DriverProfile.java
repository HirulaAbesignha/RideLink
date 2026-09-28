package com.ridelink.driver.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "driver_profiles")
public class DriverProfile {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false, unique = true)
    private UUID accountId;

    @Column(name = "service_area", nullable = false, length = 80)
    private String serviceArea;

    @Column(name = "location_name", nullable = false, length = 120)
    private String locationName;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DriverAvailability availability;

    @Column(name = "reserved_ride_id")
    private UUID reservedRideId;

    @Column(name = "reserved_at")
    private Instant reservedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DriverProfile() {
    }

    public DriverProfile(UUID accountId, String serviceArea, String locationName,
                         BigDecimal latitude, BigDecimal longitude) {
        Instant now = Instant.now();
        this.id = UUID.randomUUID();
        this.accountId = accountId;
        this.serviceArea = serviceArea;
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.availability = DriverAvailability.OFFLINE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateLocation(String serviceArea, String locationName,
                               BigDecimal latitude, BigDecimal longitude) {
        this.serviceArea = serviceArea;
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.updatedAt = Instant.now();
    }

    public void changeAvailability(DriverAvailability availability) {
        this.availability = availability;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getServiceArea() {
        return serviceArea;
    }

    public String getLocationName() {
        return locationName;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public DriverAvailability getAvailability() {
        return availability;
    }

    public UUID getReservedRideId() {
        return reservedRideId;
    }

    public Instant getReservedAt() {
        return reservedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
