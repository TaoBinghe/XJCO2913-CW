-- Upgrade existing databases to support staff-created bookings for unregistered customers.
-- Run this file once on an existing xjco2913 database.

ALTER TABLE `booking`
    ADD COLUMN `customer_name` VARCHAR(128) NULL COMMENT 'Unregistered customer contact name' AFTER `user_id`,
    ADD COLUMN `customer_email` VARCHAR(128) NULL COMMENT 'Unregistered customer contact email' AFTER `customer_name`,
    ADD COLUMN `created_by_staff_user_id` BIGINT NULL COMMENT 'Staff user ID for unregistered customer bookings' AFTER `customer_email`,
    ADD KEY `idx_booking_created_by_staff_user_id` (`created_by_staff_user_id`),
    ADD CONSTRAINT `fk_booking_staff_user` FOREIGN KEY (`created_by_staff_user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT;
