CREATE DATABASE IF NOT EXISTS goalbot
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE goalbot;

CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'User ID',
  `username` VARCHAR(64) NOT NULL COMMENT 'Login username',
  `password` VARCHAR(255) NOT NULL COMMENT 'Password hash',
  `nickname` VARCHAR(64) DEFAULT NULL COMMENT 'Display name',
  `role` VARCHAR(16) NOT NULL DEFAULT 'ADMIN' COMMENT 'Owner role',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0 disabled, 1 active',
  `last_login_at` DATETIME DEFAULT NULL COMMENT 'Last successful login time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  KEY `idx_user_role_status` (`role`, `status`),
  CONSTRAINT `ck_user_role` CHECK (`role` IN ('ADMIN', 'USER')),
  CONSTRAINT `ck_user_status` CHECK (`status` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Site owner account table';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Owner login session table';

CREATE TABLE IF NOT EXISTS `note` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Note ID',
  `user_id` BIGINT NOT NULL COMMENT 'Author ID',
  `title` VARCHAR(160) NOT NULL COMMENT 'Note title',
  `file_name` VARCHAR(255) DEFAULT NULL COMMENT 'Uploaded file name',
  `summary` VARCHAR(512) DEFAULT NULL COMMENT 'Preview summary',
  `content` LONGTEXT NOT NULL COMMENT 'Markdown content',
  `tags` VARCHAR(255) DEFAULT NULL COMMENT 'Comma separated tags',
  `category` VARCHAR(64) DEFAULT NULL COMMENT 'Knowledge base category',
  `is_published` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Whether the note is organized',
  `is_official` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Whether the note is public on the official site',
  `word_count` INT NOT NULL DEFAULT 0 COMMENT 'Approximate character count',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  KEY `idx_note_user_created` (`user_id`, `created_at`),
  KEY `idx_note_user_updated` (`user_id`, `updated_at`),
  KEY `idx_note_user_title` (`user_id`, `title`),
  KEY `idx_note_user_category_updated` (`user_id`, `category`, `updated_at`),
  KEY `idx_note_published_updated` (`is_published`, `updated_at`),
  KEY `idx_note_official_updated` (`is_official`, `is_published`, `updated_at`),
  KEY `idx_note_official_category_updated` (`is_official`, `is_published`, `category`, `updated_at`),
  CONSTRAINT `fk_note_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `ck_note_published` CHECK (`is_published` IN (0, 1)),
  CONSTRAINT `ck_note_official` CHECK (`is_official` IN (0, 1)),
  CONSTRAINT `ck_note_word_count` CHECK (`word_count` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Owner notes and public knowledge base';
