ALTER TABLE rides
    ADD COLUMN service_area VARCHAR(255),
    ADD COLUMN distance_km NUMERIC(10, 2),
    ADD COLUMN estimated_fare NUMERIC(12, 2),
    ADD COLUMN final_fare NUMERIC(12, 2),
    ADD COLUMN version BIGINT DEFAULT 0;

UPDATE rides
SET service_area = 'Colombo',
    distance_km = 115.00,
    version = 0
WHERE service_area IS NULL;

ALTER TABLE rides
    ALTER COLUMN service_area SET NOT NULL,
    ALTER COLUMN distance_km SET NOT NULL,
    ALTER COLUMN version SET NOT NULL;