USE goalbot;

ALTER TABLE `note`
  ADD COLUMN `is_published` TINYINT(1) NOT NULL DEFAULT 0
  COMMENT 'Whether this note is visible on the public blog' AFTER `tags`,
  ADD KEY `idx_note_published_updated` (`is_published`, `updated_at`);
