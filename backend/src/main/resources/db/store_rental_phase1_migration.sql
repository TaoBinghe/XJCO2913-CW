-- Upgrade existing databases from direct scooter booking to store-pickup booking.
-- Run this file once on an existing xjco2913 database.

CREATE TABLE IF NOT EXISTS `store` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(128) NOT NULL COMMENT 'Store name',
    `address` VARCHAR(255) NOT NULL COMMENT 'Store address',
    `longitude` DECIMAL(10, 6) NOT NULL COMMENT 'GCJ-02 longitude for map display',
    `latitude` DECIMAL(10, 6) NOT NULL COMMENT 'GCJ-02 latitude for map display',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT 'Status: ENABLED/DISABLED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_store_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Pickup store table';

INSERT INTO `store` (`name`, `address`, `longitude`, `latitude`, `status`) VALUES
    ('Xipu North Hub', 'Xipu Campus Library North Plaza', 103.981570, 30.768249, 'ENABLED'),
    ('Xipu South Hub', 'Xipu Campus South Gate', 103.981110, 30.764820, 'ENABLED'),
    ('Xipu Student Center Hub', 'Xipu Campus Student Center', 103.983540, 30.766980, 'ENABLED')
ON DUPLICATE KEY UPDATE
    `address` = VALUES(`address`),
    `longitude` = VALUES(`longitude`),
    `latitude` = VALUES(`latitude`),
    `status` = VALUES(`status`);

SET @north_store_id = (SELECT `id` FROM `store` WHERE `name` = 'Xipu North Hub' LIMIT 1);
SET @south_store_id = (SELECT `id` FROM `store` WHERE `name` = 'Xipu South Hub' LIMIT 1);
SET @center_store_id = (SELECT `id` FROM `store` WHERE `name` = 'Xipu Student Center Hub' LIMIT 1);

ALTER TABLE `scooter`
    ADD COLUMN `store_id` BIGINT NULL COMMENT 'Store ID' AFTER `scooter_code`,
    ADD COLUMN `lock_status` VARCHAR(32) NOT NULL DEFAULT 'LOCKED' COMMENT 'Lock status: LOCKED/UNLOCKED' AFTER `status`;

UPDATE `scooter`
SET `status` = CASE
        WHEN `status` = 'UNAVAILABLE' THEN 'DISABLED'
        ELSE `status`
    END,
    `lock_status` = 'LOCKED',
    `store_id` = CASE
        WHEN `scooter_code` IN ('SC001', 'SC002', 'SC003', 'SC004') THEN @north_store_id
        WHEN `scooter_code` IN ('SC005', 'SC006', 'SC007') THEN @south_store_id
        WHEN `scooter_code` IN ('SC008', 'SC009', 'SC010') THEN @center_store_id
        ELSE @north_store_id
    END;

UPDATE `scooter` s
INNER JOIN `store` st ON s.`store_id` = st.`id`
SET s.`location` = st.`address`,
    s.`longitude` = st.`longitude`,
    s.`latitude` = st.`latitude`;

ALTER TABLE `scooter`
    MODIFY COLUMN `status` VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE' COMMENT 'Status: AVAILABLE/IN_USE/MAINTENANCE/DISABLED',
    MODIFY COLUMN `store_id` BIGINT NOT NULL COMMENT 'Store ID',
    ADD KEY `idx_scooter_store_id` (`store_id`),
    ADD CONSTRAINT `fk_scooter_store` FOREIGN KEY (`store_id`) REFERENCES `store` (`id`) ON DELETE RESTRICT;

ALTER TABLE `booking`
    MODIFY COLUMN `scooter_id` BIGINT NULL COMMENT 'Picked scooter ID after pickup',
    ADD COLUMN `store_id` BIGINT NULL COMMENT 'Store ID' AFTER `pricing_plan_id`,
    ADD COLUMN `rental_type` VARCHAR(32) NOT NULL DEFAULT 'STORE_PICKUP' COMMENT 'Rental type' AFTER `store_id`,
    ADD COLUMN `pickup_deadline` DATETIME NULL COMMENT 'Pickup deadline' AFTER `end_time`,
    ADD COLUMN `pickup_time` DATETIME NULL COMMENT 'Actual pickup time' AFTER `pickup_deadline`,
    ADD COLUMN `return_time` DATETIME NULL COMMENT 'Actual return time' AFTER `pickup_time`,
    ADD COLUMN `overdue_cost` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'Overdue surcharge' AFTER `total_cost`;

UPDATE `booking` b
LEFT JOIN `scooter` s ON b.`scooter_id` = s.`id`
SET b.`store_id` = s.`store_id`,
    b.`rental_type` = 'STORE_PICKUP',
    b.`pickup_deadline` = DATE_ADD(b.`start_time`, INTERVAL 30 MINUTE),
    b.`pickup_time` = CASE
        WHEN b.`status` IN ('ACTIVE', 'ACTIVATED', 'COMPLETED') THEN b.`start_time`
        ELSE NULL
    END,
    b.`return_time` = CASE
        WHEN b.`status` = 'COMPLETED' THEN b.`end_time`
        ELSE NULL
    END,
    b.`overdue_cost` = 0.00,
    b.`status` = CASE
        WHEN b.`status` = 'PENDING' THEN 'RESERVED'
        WHEN b.`status` IN ('ACTIVE', 'ACTIVATED') THEN 'IN_PROGRESS'
        WHEN b.`status` = 'COMPLETED' THEN 'COMPLETED'
        WHEN b.`status` = 'CANCELLED' THEN 'CANCELLED'
        ELSE b.`status`
    END;

UPDATE `booking`
SET `store_id` = @north_store_id
WHERE `store_id` IS NULL;

ALTER TABLE `booking`
    MODIFY COLUMN `store_id` BIGINT NOT NULL COMMENT 'Store ID',
    MODIFY COLUMN `rental_type` VARCHAR(32) NOT NULL DEFAULT 'STORE_PICKUP' COMMENT 'Rental type',
    MODIFY COLUMN `pickup_deadline` DATETIME NOT NULL COMMENT 'Pickup deadline',
    MODIFY COLUMN `status` VARCHAR(32) NOT NULL DEFAULT 'RESERVED' COMMENT 'Status: RESERVED/IN_PROGRESS/OVERDUE/COMPLETED/CANCELLED/NO_SHOW_CANCELLED',
    ADD KEY `idx_booking_store_id` (`store_id`),
    ADD KEY `idx_booking_pickup_deadline` (`pickup_deadline`),
    ADD CONSTRAINT `fk_booking_store` FOREIGN KEY (`store_id`) REFERENCES `store` (`id`) ON DELETE RESTRICT;
