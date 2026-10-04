CREATE TABLE driver_profiles (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL UNIQUE,
    service_area VARCHAR(80) NOT NULL,
    location_name VARCHAR(120) NOT NULL,
    latitude DECIMAL(9, 6) NOT NULL,
    longitude DECIMAL(9, 6) NOT NULL,
    availability VARCHAR(20) NOT NULL,
    reserved_ride_id UUID,
    reserved_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_driver_availability
        CHECK (availability IN ('OFFLINE', 'AVAILABLE', 'RESERVED')),
    CONSTRAINT chk_reservation_fields
        CHECK ((availability = 'RESERVED' AND reserved_ride_id IS NOT NULL AND reserved_at IS NOT NULL)
            OR (availability <> 'RESERVED' AND reserved_ride_id IS NULL AND reserved_at IS NULL))
);

CREATE TABLE vehicles (
    id UUID PRIMARY KEY,
    driver_id UUID NOT NULL UNIQUE,
    registration_number VARCHAR(20) NOT NULL UNIQUE,
    vehicle_type VARCHAR(30) NOT NULL,
    manufacturer VARCHAR(80) NOT NULL,
    model VARCHAR(80) NOT NULL,
    colour VARCHAR(40) NOT NULL,
    seat_capacity INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_vehicle_driver FOREIGN KEY (driver_id) REFERENCES driver_profiles(id) ON DELETE CASCADE,
    CONSTRAINT chk_vehicle_type CHECK (vehicle_type IN ('CAR', 'VAN', 'THREE_WHEELER')),
    CONSTRAINT chk_vehicle_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_seat_capacity CHECK (seat_capacity BETWEEN 1 AND 12)
);

CREATE INDEX idx_driver_eligibility
    ON driver_profiles (service_area, availability);

CREATE INDEX idx_vehicle_eligibility
    ON vehicles (status, seat_capacity);
