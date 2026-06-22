USE goalbot;

CREATE TABLE IF NOT EXISTS `assistant_settings` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Assistant settings ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `proactive_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT 'Enable proactive assistant messages: 0 no, 1 yes',
  `feishu_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT 'Enable Feishu proactive messages: 0 no, 1 yes',
  `feishu_chat_id` VARCHAR(128) DEFAULT NULL COMMENT 'Optional Feishu chat_id for proactive messages; falls back to FEISHU_DEFAULT_CHAT_ID',
  `morning_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT 'Enable morning task reminder: 0 no, 1 yes',
  `morning_time` TIME NOT NULL DEFAULT '08:00:00' COMMENT 'Morning task reminder time',
  `review_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT 'Enable daily review reminder: 0 no, 1 yes',
  `review_time` TIME NOT NULL DEFAULT '22:30:00' COMMENT 'Daily review reminder time',
  `weekly_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT 'Enable weekly review reminder: 0 no, 1 yes',
  `weekly_day` TINYINT NOT NULL DEFAULT 7 COMMENT 'Weekly reminder day: 1 Monday ... 7 Sunday',
  `weekly_time` TIME NOT NULL DEFAULT '21:00:00' COMMENT 'Weekly review reminder time',
  `periodic_nudge_enabled` TINYINT NOT NULL DEFAULT 0 COMMENT 'Enable interval-based proactive planning nudges: 0 no, 1 yes',
  `periodic_nudge_interval_hours` TINYINT NOT NULL DEFAULT 3 COMMENT 'Interval hours for proactive planning nudges',
  `ai_briefing_enabled` TINYINT NOT NULL DEFAULT 0 COMMENT 'Enable daily AI briefing from external content source: 0 no, 1 yes',
  `ai_briefing_time` TIME NOT NULL DEFAULT '09:30:00' COMMENT 'Daily AI briefing time',
  `ai_briefing_source_name` VARCHAR(128) NOT NULL DEFAULT '橘鸦Juya' COMMENT 'Daily AI briefing source display name',
  `ai_briefing_source_url` VARCHAR(512) DEFAULT NULL COMMENT 'RSS, feed, or article URL for daily AI briefing',
  `advice_days` TINYINT NOT NULL DEFAULT 2 COMMENT 'Default AI advice range in days: 1 today, 2 today/tomorrow, 3 next three days',
  `quiet_enabled` TINYINT NOT NULL DEFAULT 0 COMMENT 'Enable quiet hours: 0 no, 1 yes',
  `quiet_start_time` TIME DEFAULT '23:30:00' COMMENT 'Quiet hours start time',
  `quiet_end_time` TIME DEFAULT '07:30:00' COMMENT 'Quiet hours end time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_assistant_settings_user` (`user_id`),
  KEY `idx_assistant_settings_proactive` (`proactive_enabled`, `feishu_enabled`),
  CONSTRAINT `fk_assistant_settings_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `ck_assistant_settings_bool`
    CHECK (
      `proactive_enabled` IN (0, 1)
      AND `feishu_enabled` IN (0, 1)
      AND `morning_enabled` IN (0, 1)
      AND `review_enabled` IN (0, 1)
      AND `weekly_enabled` IN (0, 1)
      AND `periodic_nudge_enabled` IN (0, 1)
      AND `ai_briefing_enabled` IN (0, 1)
      AND `quiet_enabled` IN (0, 1)
    ),
  CONSTRAINT `ck_assistant_settings_weekly_day` CHECK (`weekly_day` BETWEEN 1 AND 7),
  CONSTRAINT `ck_assistant_settings_nudge_interval` CHECK (`periodic_nudge_interval_hours` BETWEEN 1 AND 24),
  CONSTRAINT `ck_assistant_settings_advice_days` CHECK (`advice_days` BETWEEN 1 AND 3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Assistant user settings table';
