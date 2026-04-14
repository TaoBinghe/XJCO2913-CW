-- XJCO2913 E-scooter hire system - database schema
-- Database: xjco2913

-- User table
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(64) NOT NULL COMMENT 'Username',
    `password` VARCHAR(128) NOT NULL COMMENT 'Password',
    `email` VARCHAR(128) DEFAULT NULL COMMENT 'Email (optional in Sprint 1)',
    `role` VARCHAR(32) NOT NULL DEFAULT 'CUSTOMER' COMMENT 'Role: CUSTOMER/MANAGER',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0-disabled, 1-enabled',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User table';

-- E-scooter table
CREATE TABLE IF NOT EXISTS `scooter` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `scooter_code` VARCHAR(32) NOT NULL COMMENT 'Business-unique code, e.g. SC001',
    `status` VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE' COMMENT 'Status: AVAILABLE/UNAVAILABLE',
    `location` VARCHAR(128) DEFAULT NULL COMMENT 'Location name',
    `longitude` DECIMAL(10, 6) DEFAULT NULL COMMENT 'GCJ-02 longitude for map display',
    `latitude` DECIMAL(10, 6) DEFAULT NULL COMMENT 'GCJ-02 latitude for map display',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_scooter_code` (`scooter_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='E-scooter table';

-- Pricing plan table
CREATE TABLE IF NOT EXISTS `pricing_plan` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `hire_period` VARCHAR(32) NOT NULL COMMENT 'Hire period code in UNIT_NUMBER format, e.g. HOUR_1 or DAY_3',
    `price` DECIMAL(10, 2) NOT NULL COMMENT 'Price',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_hire_period` (`hire_period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Pricing plan table';

-- Booking table
CREATE TABLE IF NOT EXISTS `booking` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `scooter_id` BIGINT NOT NULL COMMENT 'Scooter ID',
    `pricing_plan_id` BIGINT NOT NULL COMMENT 'Pricing plan ID',
    `start_time` DATETIME NOT NULL COMMENT 'Start time',
    `end_time` DATETIME NOT NULL COMMENT 'End time',
    `total_cost` DECIMAL(10, 2) NOT NULL COMMENT 'Total cost',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'Status: PENDING/ACTIVATED/COMPLETED/CANCELLED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_scooter_id` (`scooter_id`),
    KEY `idx_pricing_plan_id` (`pricing_plan_id`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_booking_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_booking_scooter` FOREIGN KEY (`scooter_id`) REFERENCES `scooter` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_booking_pricing_plan` FOREIGN KEY (`pricing_plan_id`) REFERENCES `pricing_plan` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Booking table';

-- Payment table
CREATE TABLE IF NOT EXISTS `payment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `booking_id` BIGINT NOT NULL COMMENT 'Booking ID',
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `amount` DECIMAL(10, 2) NOT NULL COMMENT 'Payment amount',
    `status` VARCHAR(32) NOT NULL COMMENT 'Status: SUCCESS/FAILED',
    `card_last_four` VARCHAR(4) DEFAULT NULL COMMENT 'Last four digits of card',
    `transaction_id` VARCHAR(64) DEFAULT NULL COMMENT 'Transaction ID',
    `payment_time` DATETIME NOT NULL COMMENT 'Payment time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_booking_id` (`booking_id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_payment_booking` FOREIGN KEY (`booking_id`) REFERENCES `booking` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_payment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Payment table';

-- Initial pricing plan data
INSERT INTO `pricing_plan` (`hire_period`, `price`) VALUES
    ('HOUR_1', 5.00),
    ('HOUR_4', 15.00),
    ('DAY_1', 30.00),
    ('WEEK_1', 100.00)
ON DUPLICATE KEY UPDATE `price` = VALUES(`price`);

-- Initial scooter data (10 scooters)
INSERT INTO `scooter` (`scooter_code`, `status`, `location`, `longitude`, `latitude`) VALUES
    ('SC001', 'AVAILABLE', 'Xipu Campus Library North Plaza', 103.981570, 30.768249),
    ('SC002', 'AVAILABLE', 'Xipu Campus Library South Plaza', 103.981320, 30.767580),
    ('SC003', 'AVAILABLE', 'Xipu Campus South Gate', 103.981110, 30.764820),
    ('SC004', 'AVAILABLE', 'Xipu Campus East Gate', 103.985100, 30.768050),
    ('SC005', 'AVAILABLE', 'Xipu Campus West Gate', 103.977850, 30.768200),
    ('SC006', 'AVAILABLE', 'Xipu Campus Teaching Building 1', 103.982980, 30.769450),
    ('SC007', 'AVAILABLE', 'Xipu Campus Teaching Building 4', 103.979920, 30.769180),
    ('SC008', 'AVAILABLE', 'Xipu Campus Student Center', 103.983540, 30.766980),
    ('SC009', 'AVAILABLE', 'Xipu Campus Dormitory Area A', 103.979180, 30.766540),
    ('SC010', 'AVAILABLE', 'Xipu Campus Stadium', 103.984280, 30.770120)
ON DUPLICATE KEY UPDATE
    `status` = VALUES(`status`),
    `location` = VALUES(`location`),
    `longitude` = VALUES(`longitude`),
    `latitude` = VALUES(`latitude`);
