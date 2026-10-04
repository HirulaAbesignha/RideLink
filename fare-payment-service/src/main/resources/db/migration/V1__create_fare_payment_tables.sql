CREATE TABLE fare_estimates (
    estimate_id UUID PRIMARY KEY,
    pickup VARCHAR(255) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    distance_km NUMERIC(5,2) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    rule_version VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT chk_estimate_distance
        CHECK (distance_km >= 0.10 AND distance_km <= 100.00),

    CONSTRAINT chk_estimate_amount
        CHECK (amount >= 0),

    CONSTRAINT chk_estimate_currency
        CHECK (currency = 'LKR')
);

CREATE TABLE final_fares (
    fare_id UUID PRIMARY KEY,
    ride_id UUID NOT NULL UNIQUE,
    passenger_id UUID NOT NULL,
    distance_km NUMERIC(5,2) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    rule_version VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT chk_final_fare_distance
        CHECK (distance_km >= 0.10 AND distance_km <= 100.00),

    CONSTRAINT chk_final_fare_amount
        CHECK (amount >= 0),

    CONSTRAINT chk_final_fare_currency
        CHECK (currency = 'LKR')
);

CREATE TABLE payments (
    payment_id UUID PRIMARY KEY,
    ride_id UUID NOT NULL UNIQUE,
    amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    method_label VARCHAR(30) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    recorded_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT chk_payment_amount
        CHECK (amount >= 0),

    CONSTRAINT chk_payment_currency
        CHECK (currency = 'LKR'),

    CONSTRAINT chk_payment_status
        CHECK (status IN ('PAID', 'FAILED')),

    CONSTRAINT chk_payment_method
        CHECK (method_label IN ('SIMULATED_CARD', 'SIMULATED_CASH'))
);

CREATE TABLE receipts (
    receipt_id UUID PRIMARY KEY,
    receipt_number VARCHAR(100) NOT NULL UNIQUE,
    payment_id UUID NOT NULL UNIQUE,
    ride_id UUID NOT NULL,
    passenger_id UUID NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    method_label VARCHAR(30) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    notice VARCHAR(255) NOT NULL,

    CONSTRAINT chk_receipt_amount
        CHECK (amount >= 0),

    CONSTRAINT chk_receipt_currency
        CHECK (currency = 'LKR'),

    CONSTRAINT chk_receipt_status
        CHECK (payment_status = 'PAID')
);
