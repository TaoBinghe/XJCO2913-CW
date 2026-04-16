-- Upgrade existing store-pickup schema to support scan ride

ALTER TABLE `scooter`
    DROP FOREIGN KEY `fk_scooter_store`;

ALTER TABLE `scooter`
    MODIFY COLUMN `store_id` BIGINT DEFAULT NULL COMMENT 'Store ID for store pickup scooters',
    ADD COLUMN `rental_mode` VARCHAR(32) NOT NULL DEFAULT 'STORE_PICKUP' COMMENT 'Rental mode: STORE_PICKUP/SCAN_RIDE' AFTER `store_id`;

ALTER TABLE `scooter`
    ADD CONSTRAINT `fk_scooter_store` FOREIGN KEY (`store_id`) REFERENCES `store` (`id`) ON DELETE RESTRICT;

ALTER TABLE `booking`
    DROP FOREIGN KEY `fk_booking_store`;

ALTER TABLE `booking`
    MODIFY COLUMN `store_id` BIGINT DEFAULT NULL COMMENT 'Store ID for store pickup bookings',
    MODIFY COLUMN `rental_type` VARCHAR(32) NOT NULL DEFAULT 'STORE_PICKUP' COMMENT 'Rental type: STORE_PICKUP/SCAN_RIDE',
    MODIFY COLUMN `end_time` DATETIME DEFAULT NULL COMMENT 'Planned return time or scan ride actual return time',
    MODIFY COLUMN `pickup_deadline` DATETIME DEFAULT NULL COMMENT 'Pickup deadline for store bookings',
    ADD COLUMN `pickup_location` VARCHAR(255) DEFAULT NULL COMMENT 'Pickup location snapshot' AFTER `return_time`,
    ADD COLUMN `pickup_longitude` DECIMAL(10, 6) DEFAULT NULL COMMENT 'Pickup GCJ-02 longitude' AFTER `pickup_location`,
    ADD COLUMN `pickup_latitude` DECIMAL(10, 6) DEFAULT NULL COMMENT 'Pickup GCJ-02 latitude' AFTER `pickup_longitude`,
    ADD COLUMN `return_location` VARCHAR(255) DEFAULT NULL COMMENT 'Return location snapshot' AFTER `pickup_latitude`,
    ADD COLUMN `return_longitude` DECIMAL(10, 6) DEFAULT NULL COMMENT 'Return GCJ-02 longitude' AFTER `return_location`,
    ADD COLUMN `return_latitude` DECIMAL(10, 6) DEFAULT NULL COMMENT 'Return GCJ-02 latitude' AFTER `return_longitude`;

ALTER TABLE `booking`
    ADD CONSTRAINT `fk_booking_store` FOREIGN KEY (`store_id`) REFERENCES `store` (`id`) ON DELETE RESTRICT;

UPDATE `scooter`
SET `rental_mode` = 'STORE_PICKUP'
WHERE `rental_mode` IS NULL OR `rental_mode` = '';

INSERT INTO `pricing_plan` (`hire_period`, `price`) VALUES
    ('MINUTE_1', 1.00)
ON DUPLICATE KEY UPDATE `price` = VALUES(`price`);

INSERT INTO `scooter` (`scooter_code`, `store_id`, `rental_mode`, `status`, `lock_status`, `location`, `longitude`, `latitude`) VALUES
    ('SC201', NULL, 'SCAN_RIDE', 'AVAILABLE', 'LOCKED', 'Xipu East Roadside', 103.982120, 30.767320),
    ('SC202', NULL, 'SCAN_RIDE', 'AVAILABLE', 'LOCKED', 'Xipu South Lawn', 103.980880, 30.765210),
    ('SC203', NULL, 'SCAN_RIDE', 'AVAILABLE', 'LOCKED', 'Xipu Teaching Building Plaza', 103.983820, 30.768080),
    ('SC204', NULL, 'SCAN_RIDE', 'AVAILABLE', 'LOCKED', 'Xipu Stadium Corner', 103.984260, 30.764990),
    ('SC205', NULL, 'SCAN_RIDE', 'AVAILABLE', 'LOCKED', 'Xipu West Plaza', 103.979940, 30.766740)
ON DUPLICATE KEY UPDATE
    `store_id` = VALUES(`store_id`),
    `rental_mode` = VALUES(`rental_mode`),
    `status` = VALUES(`status`),
    `lock_status` = VALUES(`lock_status`),
    `location` = VALUES(`location`),
    `longitude` = VALUES(`longitude`),
    `latitude` = VALUES(`latitude`);
