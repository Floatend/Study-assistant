USE goalbot;

CREATE TABLE IF NOT EXISTS `note` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Note ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `title` VARCHAR(160) NOT NULL COMMENT 'Note title',
  `file_name` VARCHAR(255) DEFAULT NULL COMMENT 'Uploaded file name',
  `summary` VARCHAR(512) DEFAULT NULL COMMENT 'Preview summary',
  `content` LONGTEXT NOT NULL COMMENT 'Markdown or plain text content',
  `tags` VARCHAR(255) DEFAULT NULL COMMENT 'Comma separated tags',
  `word_count` INT NOT NULL DEFAULT 0 COMMENT 'Approximate character count',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  KEY `idx_note_user_created` (`user_id`, `created_at`),
  KEY `idx_note_user_updated` (`user_id`, `updated_at`),
  KEY `idx_note_user_title` (`user_id`, `title`),
  CONSTRAINT `fk_note_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `ck_note_word_count` CHECK (`word_count` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Personal blog note table';
