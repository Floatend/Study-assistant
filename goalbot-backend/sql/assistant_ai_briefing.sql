USE goalbot;

SET @schema_name = DATABASE();

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `assistant_settings` ADD COLUMN `ai_briefing_enabled` TINYINT NOT NULL DEFAULT 0 COMMENT ''Enable daily AI briefing from external content source: 0 no, 1 yes'' AFTER `weekly_time`',
    'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'assistant_settings'
    AND COLUMN_NAME = 'ai_briefing_enabled'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `assistant_settings` ADD COLUMN `ai_briefing_time` TIME NOT NULL DEFAULT ''09:30:00'' COMMENT ''Daily AI briefing time'' AFTER `ai_briefing_enabled`',
    'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'assistant_settings'
    AND COLUMN_NAME = 'ai_briefing_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `assistant_settings` ADD COLUMN `ai_briefing_source_name` VARCHAR(128) NOT NULL DEFAULT ''橘鸦Juya'' COMMENT ''Daily AI briefing source display name'' AFTER `ai_briefing_time`',
    'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'assistant_settings'
    AND COLUMN_NAME = 'ai_briefing_source_name'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `assistant_settings` ADD COLUMN `ai_briefing_source_url` VARCHAR(512) DEFAULT NULL COMMENT ''RSS, feed, or article URL for daily AI briefing'' AFTER `ai_briefing_source_name`',
    'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'assistant_settings'
    AND COLUMN_NAME = 'ai_briefing_source_url'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
