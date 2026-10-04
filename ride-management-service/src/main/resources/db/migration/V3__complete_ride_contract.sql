ALTER TABLE rides ADD COLUMN driver_account_id UUID;
ALTER TABLE rides ADD COLUMN seat_count INTEGER;
ALTER TABLE rides ADD COLUMN fare_estimate_id UUID;
ALTER TABLE rides ADD COLUMN currency VARCHAR(3);
ALTER TABLE rides ADD COLUMN idempotency_key UUID;
ALTER TABLE rides ADD COLUMN cancellation_reason VARCHAR(255);
ALTER TABLE rides ADD COLUMN accepted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE rides ADD COLUMN started_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE rides ADD COLUMN completed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE rides ADD COLUMN cancelled_at TIMESTAMP WITH TIME ZONE;

UPDATE rides
SET seat_count = 1,
    fare_estimate_id = id,
    currency = 'LKR',
    idempotency_key = id,
    estimated_fare = COALESCE(estimated_fare, 0),
    updated_at = COALESCE(updated_at, created_at)
WHERE seat_count IS NULL;

ALTER TABLE rides ALTER COLUMN seat_count SET NOT NULL;
ALTER TABLE rides ALTER COLUMN fare_estimate_id SET NOT NULL;
ALTER TABLE rides ALTER COLUMN currency SET NOT NULL;
ALTER TABLE rides ALTER COLUMN idempotency_key SET NOT NULL;
ALTER TABLE rides ALTER COLUMN estimated_fare SET NOT NULL;
ALTER TABLE rides ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE rides ADD CONSTRAINT chk_ride_status CHECK (status IN
    ('REQUESTED', 'ASSIGNED', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'));
ALTER TABLE rides ADD CONSTRAINT chk_ride_distance CHECK (distance_km >= 0.10 AND distance_km <= 100.00);
ALTER TABLE rides ADD CONSTRAINT chk_ride_seats CHECK (seat_count BETWEEN 1 AND 12);
ALTER TABLE rides ADD CONSTRAINT chk_ride_currency CHECK (currency = 'LKR');
ALTER TABLE rides ADD CONSTRAINT uq_ride_idempotency UNIQUE (idempotency_key);

CREATE INDEX idx_rides_passenger ON rides (rider_id, created_at DESC);
CREATE INDEX idx_rides_driver_account ON rides (driver_account_id, created_at DESC);
CREATE INDEX idx_rides_status ON rides (status, created_at DESC);
