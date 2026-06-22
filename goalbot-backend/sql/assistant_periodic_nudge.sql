USE goalbot;

SET @schema_name = DATABASE();

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `assistant_settings` ADD COLUMN `periodic_nudge_enabled` TINYINT NOT NULL DEFAULT 0 COMMENT ''Enable interval-based proactive planning nudges: 0 no, 1 yes'' AFTER `weekly_time`',
    'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'assistant_settings'
    AND COLUMN_NAME = 'periodic_nudge_enabled'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `assistant_settings` ADD COLUMN `periodic_nudge_interval_hours` TINYINT NOT NULL DEFAULT 3 COMMENT ''Interval hours for proactive planning nudges'' AFTER `periodic_nudge_enabled`',
    'SELECT 1')
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'assistant_settings'
    AND COLUMN_NAME = 'periodic_nudge_interval_hours'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) > 0,
    'ALTER TABLE `assistant_settings` DROP CHECK `ck_assistant_settings_bool`',
    'SELECT 1')
  FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'assistant_settings'
    AND CONSTRAINT_NAME = 'ck_assistant_settings_bool'
    AND CONSTRAINT_TYPE = 'CHECK'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE `assistant_settings`
  ADD CONSTRAINT `ck_assistant_settings_bool`
    CHECK (
      `proactive_enabled` IN (0, 1)
      AND `feishu_enabled` IN (0, 1)
      AND `morning_enabled` IN (0, 1)
      AND `review_enabled` IN (0, 1)
      AND `weekly_enabled` IN (0, 1)
      AND `periodic_nudge_enabled` IN (0, 1)
      AND `ai_briefing_enabled` IN (0, 1)
      AND `quiet_enabled` IN (0, 1)
    );

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `assistant_settings` ADD CONSTRAINT `ck_assistant_settings_nudge_interval` CHECK (`periodic_nudge_interval_hours` BETWEEN 1 AND 24)',
    'SELECT 1')
  FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'assistant_settings'
    AND CONSTRAINT_NAME = 'ck_assistant_settings_nudge_interval'
    AND CONSTRAINT_TYPE = 'CHECK'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
