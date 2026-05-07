-- Upgrade existing databases with indexes used by high-concurrency booking/payment flows.
-- Run this file once on an existing xjco2913 database.

ALTER TABLE `scooter`
    ADD KEY `idx_scooter_store_mode_status` (`store_id`, `rental_mode`, `status`);

ALTER TABLE `booking`
    ADD KEY `idx_booking_user_status` (`user_id`, `status`),
    ADD KEY `idx_booking_store_window` (`store_id`, `rental_type`, `status`, `start_time`, `end_time`);

ALTER TABLE `payment`
    ADD KEY `idx_payment_user_status_time` (`user_id`, `status`, `payment_time`);
