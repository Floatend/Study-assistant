USE goalbot;

SET @schema_name = DATABASE();

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'user' AND COLUMN_NAME = 'role'),
  'SELECT 1',
  'ALTER TABLE `user` ADD COLUMN `role` VARCHAR(16) NOT NULL DEFAULT ''USER'' COMMENT ''Role: ADMIN or USER'' AFTER `feishu_user_id`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'user' AND COLUMN_NAME = 'status'),
  'SELECT 1',
  'ALTER TABLE `user` ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT ''Status: 0 disabled, 1 active'' AFTER `role`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'user' AND COLUMN_NAME = 'last_login_at'),
  'SELECT 1',
  'ALTER TABLE `user` ADD COLUMN `last_login_at` DATETIME DEFAULT NULL COMMENT ''Last successful login time'' AFTER `status`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'user' AND INDEX_NAME = 'idx_user_role_status'),
  'SELECT 1',
  'ALTER TABLE `user` ADD KEY `idx_user_role_status` (`role`, `status`)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `auth_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Session ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `token_hash` CHAR(64) NOT NULL COMMENT 'SHA-256 hash of bearer token',
  `expires_at` DATETIME NOT NULL COMMENT 'Session expiration time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `last_accessed_at` DATETIME DEFAULT NULL COMMENT 'Last access time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_session_token_hash` (`token_hash`),
  KEY `idx_auth_session_user` (`user_id`, `expires_at`),
  KEY `idx_auth_session_expires` (`expires_at`),
  CONSTRAINT `fk_auth_session_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Web login session table';

UPDATE `user`
SET `role` = 'ADMIN', `status` = 1
WHERE `id` = 1 OR `username` = 'local_user';
