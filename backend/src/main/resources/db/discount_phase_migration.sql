-- Upgrade existing databases to support customer discount eligibility and payment discount records.
-- Run this file once on an existing xjco2913 database.

ALTER TABLE `user`
    ADD COLUMN `customer_type` VARCHAR(32) NOT NULL DEFAULT 'REGULAR' COMMENT 'Customer type: REGULAR/STUDENT/SENIOR' AFTER `email`;

ALTER TABLE `payment`
    ADD COLUMN `original_amount` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'Original amount before discount' AFTER `user_id`,
    ADD COLUMN `discount_type` VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT 'Discount type: NONE/STUDENT/SENIOR/FREQUENT_USER' AFTER `original_amount`,
    ADD COLUMN `discount_rate` DECIMAL(5, 4) NOT NULL DEFAULT 0.0000 COMMENT 'Discount rate applied to original amount' AFTER `discount_type`,
    ADD COLUMN `discount_amount` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'Discount amount' AFTER `discount_rate`;

UPDATE `payment`
SET `original_amount` = `amount`
WHERE `original_amount` = 0.00;
