-- Upgrade existing databases for safer bank-card storage and BCrypt card passwords.
-- Run this file once on an existing xjco2913 database before deploying the updated backend.

ALTER TABLE `user`
    MODIFY COLUMN `password` VARCHAR(128) NOT NULL COMMENT 'BCrypt or legacy MD5 password hash';

ALTER TABLE `bank_card`
    MODIFY COLUMN `card_number` VARCHAR(32) NULL COMMENT 'Legacy full card number for older records',
    MODIFY COLUMN `password_hash` VARCHAR(128) NOT NULL COMMENT 'BCrypt or legacy MD5 hash of card password',
    ADD COLUMN `card_fingerprint` VARCHAR(128) NULL COMMENT 'One-way card fingerprint for duplicate detection' AFTER `card_last_four`,
    ADD UNIQUE KEY `uk_bank_card_user_fingerprint` (`user_id`, `card_fingerprint`);
