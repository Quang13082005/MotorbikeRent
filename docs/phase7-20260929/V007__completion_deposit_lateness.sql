ALTER TABLE rental_contracts ADD COLUMN asset_deposit_held int NULL DEFAULT 0;
ALTER TABLE rental_contracts ADD COLUMN asset_deposit_refund_amount int NULL DEFAULT 0;
ALTER TABLE rental_contracts ADD COLUMN asset_deposit_deduction_amount int NULL DEFAULT 0;
ALTER TABLE rental_contracts ADD COLUMN deposit_deduction_reason text NULL;
ALTER TABLE rental_contracts ADD COLUMN late_fee int NULL DEFAULT 0;
ALTER TABLE rental_contracts ADD COLUMN late_fee_reason text NULL;
ALTER TABLE rental_contracts ADD COLUMN late_fee_payment_method varchar(50) NULL;
