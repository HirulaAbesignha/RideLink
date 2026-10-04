package com.ridelink.driver.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    private UUID id;

    @Column(name = "driver_id", nullable = false, unique = true)
    private UUID driverId;

    @Column(name = "registration_number", nullable = false, unique = true, length = 20)
    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 30)
    private VehicleType vehicleType;

    @Column(nullable = false, length = 80)
    private String manufacturer;

    @Column(nullable = false, length = 80)
    private String model;

    @Column(nullable = false, length = 40)
    private String colour;

    @Column(name = "seat_capacity", nullable = false)
    private int seatCapacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleStatus status;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Vehicle() {
    }

    public Vehicle(UUID driverId, String registrationNumber, VehicleType vehicleType,
                   String manufacturer, String model, String colour,
                   int seatCapacity, VehicleStatus status) {
        Instant now = Instant.now();
        this.id = UUID.randomUUID();
        this.driverId = driverId;
        this.createdAt = now;
        update(registrationNumber, vehicleType, manufacturer, model, colour, seatCapacity, status);
    }

    public void update(String registrationNumber, VehicleType vehicleType,
                       String manufacturer, String model, String colour,
                       int seatCapacity, VehicleStatus status) {
        this.registrationNumber = registrationNumber;
        this.vehicleType = vehicleType;
        this.manufacturer = manufacturer;
        this.model = model;
        this.colour = colour;
        this.seatCapacity = seatCapacity;
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getDriverId() {
        return driverId;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModel() {
        return model;
    }

    public String getColour() {
        return colour;
    }

    public int getSeatCapacity() {
        return seatCapacity;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
