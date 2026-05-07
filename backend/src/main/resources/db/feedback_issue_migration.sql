-- Upgrade existing databases to support customer feedback and fault issue handling.
-- Run this file once on an existing xjco2913 database.

CREATE TABLE IF NOT EXISTS `feedback_issue` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT 'Submitting user ID',
    `booking_id` BIGINT NOT NULL COMMENT 'Related booking ID',
    `scooter_id` BIGINT DEFAULT NULL COMMENT 'Related scooter ID when available',
    `category` VARCHAR(32) NOT NULL COMMENT 'Category: SCOOTER_FAULT/BOOKING/PAYMENT/OTHER',
    `content` VARCHAR(500) NOT NULL COMMENT 'Short feedback content',
    `priority` VARCHAR(16) NOT NULL DEFAULT 'LOW' COMMENT 'Priority: LOW/HIGH',
    `status` VARCHAR(32) NOT NULL DEFAULT 'OPEN' COMMENT 'Status: OPEN/IN_PROGRESS/RESOLVED',
    `resolution_note` VARCHAR(500) DEFAULT NULL COMMENT 'Admin handling note',
    `handled_by_user_id` BIGINT DEFAULT NULL COMMENT 'Manager user ID that last handled the issue',
    `resolved_at` DATETIME DEFAULT NULL COMMENT 'Resolved time',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_feedback_issue_user_id` (`user_id`),
    KEY `idx_feedback_issue_booking_id` (`booking_id`),
    KEY `idx_feedback_issue_scooter_id` (`scooter_id`),
    KEY `idx_feedback_issue_priority` (`priority`),
    KEY `idx_feedback_issue_status` (`status`),
    KEY `idx_feedback_issue_handled_by_user_id` (`handled_by_user_id`),
    CONSTRAINT `fk_feedback_issue_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_feedback_issue_booking` FOREIGN KEY (`booking_id`) REFERENCES `booking` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_feedback_issue_scooter` FOREIGN KEY (`scooter_id`) REFERENCES `scooter` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_feedback_issue_handler` FOREIGN KEY (`handled_by_user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Customer feedback and fault issue table';
