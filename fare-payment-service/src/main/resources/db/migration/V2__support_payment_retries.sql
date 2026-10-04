-- Preserve existing payments while allowing another attempt after a failed simulation.
ALTER TABLE payments ADD COLUMN passenger_id UUID;

-- Backfill the owner for successful payments that already have a receipt.
UPDATE payments p
SET passenger_id = (
    SELECT r.passenger_id
    FROM receipts r
    WHERE r.payment_id = p.payment_id
)
WHERE EXISTS (
    SELECT 1
    FROM receipts r
    WHERE r.payment_id = p.payment_id
);

-- Failed legacy payments have no receipt, so their owner remains unknown and
-- the application denies idempotent replay for those rows.
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_ride_id_key;

ALTER TABLE payments ADD COLUMN successful_ride_id UUID;

UPDATE payments
SET successful_ride_id = ride_id
WHERE status = 'PAID';

ALTER TABLE payments
    ADD CONSTRAINT uq_payments_one_success_per_ride UNIQUE (successful_ride_id);
