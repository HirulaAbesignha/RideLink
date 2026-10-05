package com.ridelink.ride.entity;

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
@Table(name = "rides")
public class Ride {

    @Id
    private UUID id;

    @Column(name = "rider_id", nullable = false)
    private UUID passengerId;

    @Column(name = "driver_id")
    private UUID driverId;

    @Column(name = "driver_account_id")
    private UUID driverAccountId;

    @Column(name = "pickup_location", nullable = false, length = 255)
    private String pickup;

    @Column(nullable = false, length = 255)
    private String destination;

    @Column(name = "service_area", nullable = false, length = 80)
    private String serviceArea;

    @Column(name = "distance_km", nullable = false, precision = 5, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "seat_count", nullable = false)
    private int seatCount;

    @Column(name = "fare_estimate_id", nullable = false)
    private UUID fareEstimateId;

    @Column(name = "estimated_fare", nullable = false, precision = 12, scale = 2)
    private BigDecimal estimatedFare;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "final_fare", precision = 12, scale = 2)
    private BigDecimal finalFare;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RideStatus status;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private UUID idempotencyKey;

    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Ride() {
    }

    public Ride(UUID passengerId, String pickup, String destination, String serviceArea,
                BigDecimal distanceKm, int seatCount, UUID fareEstimateId,
                BigDecimal estimatedFare, String currency, UUID idempotencyKey) {
        Instant now = Instant.now();
        this.id = UUID.randomUUID();
        this.passengerId = passengerId;
        this.pickup = pickup;
        this.destination = destination;
        this.serviceArea = serviceArea;
        this.distanceKm = distanceKm;
        this.seatCount = seatCount;
        this.fareEstimateId = fareEstimateId;
        this.estimatedFare = estimatedFare;
        this.currency = currency;
        this.status = RideStatus.REQUESTED;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void assign(UUID driverId, UUID driverAccountId) {
        this.driverId = driverId;
        this.driverAccountId = driverAccountId;
        this.status = RideStatus.ASSIGNED;
        touch();
    }

    public void accept() {
        this.status = RideStatus.ACCEPTED;
        this.acceptedAt = Instant.now();
        touch();
    }

    public void start() {
        this.status = RideStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
        touch();
    }

    public void complete(BigDecimal finalFare) {
        this.finalFare = finalFare;
        this.status = RideStatus.COMPLETED;
        this.completedAt = Instant.now();
        touch();
    }

    public void cancel(String reason) {
        this.status = RideStatus.CANCELLED;
        this.cancellationReason = reason;
        this.cancelledAt = Instant.now();
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getPassengerId() { return passengerId; }
    public UUID getDriverId() { return driverId; }
    public UUID getDriverAccountId() { return driverAccountId; }
    public String getPickup() { return pickup; }
    public String getDestination() { return destination; }
    public String getServiceArea() { return serviceArea; }
    public BigDecimal getDistanceKm() { return distanceKm; }
    public int getSeatCount() { return seatCount; }
    public UUID getFareEstimateId() { return fareEstimateId; }
    public BigDecimal getEstimatedFare() { return estimatedFare; }
    public String getCurrency() { return currency; }
    public BigDecimal getFinalFare() { return finalFare; }
    public RideStatus getStatus() { return status; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public String getCancellationReason() { return cancellationReason; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
