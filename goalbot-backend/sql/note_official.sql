USE goalbot;

-- This migration is intentionally repeatable. It supports databases that missed the
-- previous public-note migration as well as databases that already have it.
SET @schema_name = DATABASE();

SET @migration_sql = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = @schema_name AND table_name = 'note' AND column_name = 'is_published'
    ),
    'SELECT 1',
    'ALTER TABLE `note` ADD COLUMN `is_published` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''Whether this note is visible on the public blog'' AFTER `tags`'
  )
);
PREPARE note_migration FROM @migration_sql;
EXECUTE note_migration;
DEALLOCATE PREPARE note_migration;

SET @migration_sql = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = @schema_name AND table_name = 'note' AND column_name = 'is_official'
    ),
    'SELECT 1',
    'ALTER TABLE `note` ADD COLUMN `is_official` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''Whether this note is approved for the official site'' AFTER `is_published`'
  )
);
PREPARE note_migration FROM @migration_sql;
EXECUTE note_migration;
DEALLOCATE PREPARE note_migration;

SET @migration_sql = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.statistics
      WHERE table_schema = @schema_name AND table_name = 'note' AND index_name = 'idx_note_published_updated'
    ),
    'SELECT 1',
    'ALTER TABLE `note` ADD KEY `idx_note_published_updated` (`is_published`, `updated_at`)'
  )
);
PREPARE note_migration FROM @migration_sql;
EXECUTE note_migration;
DEALLOCATE PREPARE note_migration;

SET @migration_sql = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.statistics
      WHERE table_schema = @schema_name AND table_name = 'note' AND index_name = 'idx_note_official_updated'
    ),
    'SELECT 1',
    'ALTER TABLE `note` ADD KEY `idx_note_official_updated` (`is_official`, `is_published`, `updated_at`)'
  )
);
PREPARE note_migration FROM @migration_sql;
EXECUTE note_migration;
DEALLOCATE PREPARE note_migration;
