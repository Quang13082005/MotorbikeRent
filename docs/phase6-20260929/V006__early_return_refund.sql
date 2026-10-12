-- Phase 6: Early return and partial rental refund audit fields
ALTER TABLE rental_contracts ADD COLUMN early_return_requested_at DATETIME(6) NULL;
ALTER TABLE rental_contracts ADD COLUMN early_return_reason TEXT NULL;
ALTER TABLE rental_contracts ADD COLUMN early_return_refund_amount INT NULL DEFAULT 0;
ALTER TABLE rental_contracts ADD COLUMN early_return_decision_reason TEXT NULL;
ALTER TABLE rental_contracts ADD COLUMN early_return_payment_method VARCHAR(50) NULL;
