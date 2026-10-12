ALTER TABLE rental_extensions ADD COLUMN motorbike_id BIGINT NULL;
ALTER TABLE rental_extensions ADD COLUMN daily_rate INT NULL;
ALTER TABLE rental_extensions ADD COLUMN original_total INT NULL;
ALTER TABLE rental_extensions ADD COLUMN expires_at DATETIME(6) NULL;
ALTER TABLE rental_extensions ADD COLUMN approved_at DATETIME(6) NULL;
ALTER TABLE rental_extensions ADD COLUMN processed_at DATETIME(6) NULL;
UPDATE rental_extensions e JOIN rental_orders o ON o.order_id=e.order_id SET e.motorbike_id=o.motorbike_id,e.daily_rate=o.daily_rate,e.original_total=o.total_price WHERE e.status='PENDING' AND e.motorbike_id IS NULL;
