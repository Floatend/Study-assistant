USE goalbot;

-- Repeatable migration for the notebook and public knowledge-base upgrade.
SET @schema_name = DATABASE();

SET @migration_sql = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = @schema_name AND table_name = 'note' AND column_name = 'category'
    ),
    'SELECT 1',
    'ALTER TABLE `note` ADD COLUMN `category` VARCHAR(64) DEFAULT NULL COMMENT ''Knowledge base category'' AFTER `tags`'
  )
);
PREPARE note_knowledge_base_migration FROM @migration_sql;
EXECUTE note_knowledge_base_migration;
DEALLOCATE PREPARE note_knowledge_base_migration;

SET @migration_sql = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.statistics
      WHERE table_schema = @schema_name AND table_name = 'note' AND index_name = 'idx_note_user_category_updated'
    ),
    'SELECT 1',
    'ALTER TABLE `note` ADD KEY `idx_note_user_category_updated` (`user_id`, `category`, `updated_at`)'
  )
);
PREPARE note_knowledge_base_migration FROM @migration_sql;
EXECUTE note_knowledge_base_migration;
DEALLOCATE PREPARE note_knowledge_base_migration;

SET @migration_sql = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.statistics
      WHERE table_schema = @schema_name AND table_name = 'note' AND index_name = 'idx_note_official_category_updated'
    ),
    'SELECT 1',
    'ALTER TABLE `note` ADD KEY `idx_note_official_category_updated` (`is_official`, `is_published`, `category`, `updated_at`)'
  )
);
PREPARE note_knowledge_base_migration FROM @migration_sql;
EXECUTE note_knowledge_base_migration;
DEALLOCATE PREPARE note_knowledge_base_migration;
