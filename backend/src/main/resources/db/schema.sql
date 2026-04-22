-- XJCO2913 store-pickup scooter rental system - latest schema

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(64) NOT NULL COMMENT 'Username',
    `password` VARCHAR(128) NOT NULL COMMENT 'Password',
    `email` VARCHAR(128) DEFAULT NULL COMMENT 'Email',
    `role` VARCHAR(32) NOT NULL DEFAULT 'CUSTOMER' COMMENT 'Role: CUSTOMER/MANAGER',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0-disabled, 1-enabled',
    `wallet_balance` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'Wallet balance',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User table';

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

CREATE TABLE IF NOT EXISTS `scooter` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `scooter_code` VARCHAR(32) NOT NULL COMMENT 'Business-unique code, e.g. SC001',
    `store_id` BIGINT DEFAULT NULL COMMENT 'Store ID for store pickup scooters',
    `rental_mode` VARCHAR(32) NOT NULL DEFAULT 'STORE_PICKUP' COMMENT 'Rental mode: STORE_PICKUP/SCAN_RIDE',
    `status` VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE' COMMENT 'Status: AVAILABLE/IN_USE/MAINTENANCE/DISABLED',
    `lock_status` VARCHAR(32) NOT NULL DEFAULT 'LOCKED' COMMENT 'Lock status: LOCKED/UNLOCKED',
    `location` VARCHAR(255) DEFAULT NULL COMMENT 'Current location snapshot',
    `longitude` DECIMAL(10, 6) DEFAULT NULL COMMENT 'Current GCJ-02 longitude for map display',
    `latitude` DECIMAL(10, 6) DEFAULT NULL COMMENT 'Current GCJ-02 latitude for map display',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_scooter_code` (`scooter_code`),
    KEY `idx_scooter_store_id` (`store_id`),
    CONSTRAINT `fk_scooter_store` FOREIGN KEY (`store_id`) REFERENCES `store` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Scooter table';

CREATE TABLE IF NOT EXISTS `pricing_plan` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `hire_period` VARCHAR(32) NOT NULL COMMENT 'Hire period code in UNIT_NUMBER format, e.g. HOUR_1 or DAY_3',
    `price` DECIMAL(10, 2) NOT NULL COMMENT 'Price',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_hire_period` (`hire_period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Pricing plan table';

CREATE TABLE IF NOT EXISTS `booking` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `scooter_id` BIGINT DEFAULT NULL COMMENT 'Picked scooter ID after pickup',
    `pricing_plan_id` BIGINT NOT NULL COMMENT 'Pricing plan ID',
    `store_id` BIGINT DEFAULT NULL COMMENT 'Store ID for store pickup bookings',
    `rental_type` VARCHAR(32) NOT NULL DEFAULT 'STORE_PICKUP' COMMENT 'Rental type: STORE_PICKUP/SCAN_RIDE',
    `start_time` DATETIME NOT NULL COMMENT 'Appointment start time or scan ride start time',
    `end_time` DATETIME DEFAULT NULL COMMENT 'Planned return time or scan ride actual return time',
    `pickup_deadline` DATETIME DEFAULT NULL COMMENT 'Pickup deadline for store bookings',
    `pickup_time` DATETIME DEFAULT NULL COMMENT 'Actual pickup time',
    `return_time` DATETIME DEFAULT NULL COMMENT 'Actual return time',
    `pickup_location` VARCHAR(255) DEFAULT NULL COMMENT 'Pickup location snapshot',
    `pickup_longitude` DECIMAL(10, 6) DEFAULT NULL COMMENT 'Pickup GCJ-02 longitude',
    `pickup_latitude` DECIMAL(10, 6) DEFAULT NULL COMMENT 'Pickup GCJ-02 latitude',
    `return_location` VARCHAR(255) DEFAULT NULL COMMENT 'Return location snapshot',
    `return_longitude` DECIMAL(10, 6) DEFAULT NULL COMMENT 'Return GCJ-02 longitude',
    `return_latitude` DECIMAL(10, 6) DEFAULT NULL COMMENT 'Return GCJ-02 latitude',
    `total_cost` DECIMAL(10, 2) NOT NULL COMMENT 'Final total cost',
    `overdue_cost` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'Overdue surcharge',
    `status` VARCHAR(32) NOT NULL DEFAULT 'RESERVED' COMMENT 'Status: RESERVED/IN_PROGRESS/OVERDUE/AWAITING_PAYMENT/COMPLETED/CANCELLED/NO_SHOW_CANCELLED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_booking_user_id` (`user_id`),
    KEY `idx_booking_scooter_id` (`scooter_id`),
    KEY `idx_booking_pricing_plan_id` (`pricing_plan_id`),
    KEY `idx_booking_store_id` (`store_id`),
    KEY `idx_booking_status` (`status`),
    KEY `idx_booking_pickup_deadline` (`pickup_deadline`),
    CONSTRAINT `fk_booking_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_booking_scooter` FOREIGN KEY (`scooter_id`) REFERENCES `scooter` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_booking_pricing_plan` FOREIGN KEY (`pricing_plan_id`) REFERENCES `pricing_plan` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_booking_store` FOREIGN KEY (`store_id`) REFERENCES `store` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Store pickup booking table';

CREATE TABLE IF NOT EXISTS `payment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `booking_id` BIGINT NOT NULL COMMENT 'Booking ID',
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `amount` DECIMAL(10, 2) NOT NULL COMMENT 'Payment amount',
    `status` VARCHAR(32) NOT NULL COMMENT 'Status: SUCCESS/FAILED',
    `payment_method` VARCHAR(32) NOT NULL DEFAULT 'CARD' COMMENT 'Payment method: WALLET/CARD',
    `card_last_four` VARCHAR(4) DEFAULT NULL COMMENT 'Last four digits of card',
    `transaction_id` VARCHAR(64) DEFAULT NULL COMMENT 'Transaction ID',
    `payment_time` DATETIME NOT NULL COMMENT 'Payment time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_booking_id` (`booking_id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_payment_booking` FOREIGN KEY (`booking_id`) REFERENCES `booking` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_payment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Payment table';

CREATE TABLE IF NOT EXISTS `bank_card` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `bank_name` VARCHAR(64) NOT NULL COMMENT 'Bank name',
    `holder_name` VARCHAR(64) NOT NULL COMMENT 'Card holder name',
    `card_number` VARCHAR(32) NOT NULL COMMENT 'Card number',
    `card_last_four` VARCHAR(4) NOT NULL COMMENT 'Last four digits of card',
    `password_hash` VARCHAR(32) NOT NULL COMMENT 'MD5 hash of card password',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_bank_card_user_card_number` (`user_id`, `card_number`),
    KEY `idx_bank_card_user_id` (`user_id`),
    CONSTRAINT `fk_bank_card_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bound bank card table';

CREATE TABLE IF NOT EXISTS `wallet_transaction` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `type` VARCHAR(32) NOT NULL COMMENT 'Transaction type: RECHARGE/BOOKING_PAYMENT',
    `amount` DECIMAL(10, 2) NOT NULL COMMENT 'Transaction amount',
    `balance_after` DECIMAL(10, 2) NOT NULL COMMENT 'Wallet balance after transaction',
    `booking_id` BIGINT DEFAULT NULL COMMENT 'Booking ID for booking payments',
    `bank_card_id` BIGINT DEFAULT NULL COMMENT 'Bank card ID for recharges',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_wallet_transaction_user_id` (`user_id`),
    KEY `idx_wallet_transaction_booking_id` (`booking_id`),
    KEY `idx_wallet_transaction_bank_card_id` (`bank_card_id`),
    CONSTRAINT `fk_wallet_transaction_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_wallet_transaction_booking` FOREIGN KEY (`booking_id`) REFERENCES `booking` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_wallet_transaction_bank_card` FOREIGN KEY (`bank_card_id`) REFERENCES `bank_card` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Wallet transaction table';

INSERT INTO `store` (`id`, `name`, `address`, `longitude`, `latitude`, `status`) VALUES
    (1, 'Xipu North Hub', 'Xipu Campus Library North Plaza', 103.981570, 30.768249, 'ENABLED'),
    (2, 'Xipu South Hub', 'Xipu Campus South Gate', 103.981110, 30.764820, 'ENABLED'),
    (3, 'Xipu Student Center Hub', 'Xipu Campus Student Center', 103.983540, 30.766980, 'ENABLED')
ON DUPLICATE KEY UPDATE
    `address` = VALUES(`address`),
    `longitude` = VALUES(`longitude`),
    `latitude` = VALUES(`latitude`),
    `status` = VALUES(`status`);

INSERT INTO `pricing_plan` (`hire_period`, `price`) VALUES
    ('MINUTE_1', 1.00),
    ('HOUR_1', 5.00),
    ('HOUR_4', 15.00),
    ('DAY_1', 30.00),
    ('WEEK_1', 100.00)
ON DUPLICATE KEY UPDATE `price` = VALUES(`price`);

INSERT INTO `scooter` (`scooter_code`, `store_id`, `rental_mode`, `status`, `lock_status`, `location`, `longitude`, `latitude`) VALUES
    ('SC001', 1, 'STORE_PICKUP', 'AVAILABLE', 'LOCKED', 'Xipu Campus Library North Plaza', 103.981570, 30.768249),
    ('SC002', 1, 'STORE_PICKUP', 'AVAILABLE', 'LOCKED', 'Xipu Campus Library North Plaza', 103.981570, 30.768249),
    ('SC003', 1, 'STORE_PICKUP', 'AVAILABLE', 'LOCKED', 'Xipu Campus Library North Plaza', 103.981570, 30.768249),
    ('SC004', 1, 'STORE_PICKUP', 'AVAILABLE', 'LOCKED', 'Xipu Campus Library North Plaza', 103.981570, 30.768249),
    ('SC005', 2, 'STORE_PICKUP', 'AVAILABLE', 'LOCKED', 'Xipu Campus South Gate', 103.981110, 30.764820),
    ('SC006', 2, 'STORE_PICKUP', 'AVAILABLE', 'LOCKED', 'Xipu Campus South Gate', 103.981110, 30.764820),
    ('SC007', 2, 'STORE_PICKUP', 'AVAILABLE', 'LOCKED', 'Xipu Campus South Gate', 103.981110, 30.764820),
    ('SC008', 3, 'STORE_PICKUP', 'AVAILABLE', 'LOCKED', 'Xipu Campus Student Center', 103.983540, 30.766980),
    ('SC009', 3, 'STORE_PICKUP', 'AVAILABLE', 'LOCKED', 'Xipu Campus Student Center', 103.983540, 30.766980),
    ('SC010', 3, 'STORE_PICKUP', 'AVAILABLE', 'LOCKED', 'Xipu Campus Student Center', 103.983540, 30.766980),
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
