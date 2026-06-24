CREATE DATABASE IF NOT EXISTS goalbot
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE goalbot;

CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'User ID',
  `username` VARCHAR(64) NOT NULL COMMENT 'Login username',
  `password` VARCHAR(255) NOT NULL COMMENT 'Password hash',
  `nickname` VARCHAR(64) DEFAULT NULL COMMENT 'Nickname',
  `feishu_user_id` VARCHAR(128) DEFAULT NULL COMMENT 'Feishu user ID',
  `role` VARCHAR(16) NOT NULL DEFAULT 'USER' COMMENT 'Role: ADMIN or USER',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0 disabled, 1 active',
  `last_login_at` DATETIME DEFAULT NULL COMMENT 'Last successful login time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_feishu_user_id` (`feishu_user_id`),
  KEY `idx_user_role_status` (`role`, `status`),
  CONSTRAINT `ck_user_role` CHECK (`role` IN ('ADMIN', 'USER')),
  CONSTRAINT `ck_user_status` CHECK (`status` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User table';

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

CREATE TABLE IF NOT EXISTS `goal` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Goal ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `title` VARCHAR(128) NOT NULL COMMENT 'Goal title',
  `description` TEXT DEFAULT NULL COMMENT 'Goal description',
  `start_date` DATE DEFAULT NULL COMMENT 'Start date',
  `end_date` DATE DEFAULT NULL COMMENT 'End date',
  `priority` TINYINT NOT NULL DEFAULT 2 COMMENT 'Priority: 1 low, 2 medium, 3 high, 4 urgent',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT 'Status: 0 not started, 1 active, 2 paused, 3 completed, 4 archived',
  `progress` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT 'Progress percentage',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  KEY `idx_goal_user_id` (`user_id`),
  KEY `idx_goal_user_status` (`user_id`, `status`),
  KEY `idx_goal_user_priority` (`user_id`, `priority`),
  KEY `idx_goal_end_date` (`end_date`),
  CONSTRAINT `fk_goal_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `ck_goal_priority` CHECK (`priority` IN (1, 2, 3, 4)),
  CONSTRAINT `ck_goal_status` CHECK (`status` IN (0, 1, 2, 3, 4)),
  CONSTRAINT `ck_goal_progress` CHECK (`progress` >= 0 AND `progress` <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Goal table';

CREATE TABLE IF NOT EXISTS `task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Task ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `goal_id` BIGINT DEFAULT NULL COMMENT 'Goal ID',
  `title` VARCHAR(128) NOT NULL COMMENT 'Task title',
  `description` TEXT DEFAULT NULL COMMENT 'Task description',
  `plan_date` DATE NOT NULL COMMENT 'Plan date',
  `start_time` TIME DEFAULT NULL COMMENT 'Start time',
  `end_time` TIME DEFAULT NULL COMMENT 'End time',
  `planned_minutes` INT NOT NULL DEFAULT 0 COMMENT 'Planned minutes',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT 'Status: 0 pending, 2 completed',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  KEY `idx_task_user_id` (`user_id`),
  KEY `idx_task_goal_id` (`goal_id`),
  KEY `idx_task_user_plan_date` (`user_id`, `plan_date`),
  KEY `idx_task_user_status` (`user_id`, `status`),
  KEY `idx_task_calendar` (`user_id`, `plan_date`, `start_time`),
  CONSTRAINT `fk_task_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_task_goal`
    FOREIGN KEY (`goal_id`) REFERENCES `goal` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
  CONSTRAINT `ck_task_planned_minutes` CHECK (`planned_minutes` >= 0),
  CONSTRAINT `ck_task_status` CHECK (`status` IN (0, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Task table';

CREATE TABLE IF NOT EXISTS `checkin` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Checkin ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `task_id` BIGINT DEFAULT NULL COMMENT 'Task ID',
  `actual_minutes` INT NOT NULL DEFAULT 0 COMMENT 'Actual minutes',
  `content` TEXT DEFAULT NULL COMMENT 'Checkin content',
  `mood` TINYINT DEFAULT NULL COMMENT 'Mood: 1 very bad, 2 bad, 3 normal, 4 good, 5 great',
  `difficulty` TINYINT DEFAULT NULL COMMENT 'Difficulty: 1 very easy, 2 easy, 3 normal, 4 hard, 5 very hard',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  PRIMARY KEY (`id`),
  KEY `idx_checkin_user_id` (`user_id`),
  KEY `idx_checkin_task_id` (`task_id`),
  KEY `idx_checkin_user_created_at` (`user_id`, `created_at`),
  CONSTRAINT `fk_checkin_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_checkin_task`
    FOREIGN KEY (`task_id`) REFERENCES `task` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
  CONSTRAINT `ck_checkin_actual_minutes` CHECK (`actual_minutes` >= 0),
  CONSTRAINT `ck_checkin_mood` CHECK (`mood` IS NULL OR `mood` IN (1, 2, 3, 4, 5)),
  CONSTRAINT `ck_checkin_difficulty` CHECK (`difficulty` IS NULL OR `difficulty` IN (1, 2, 3, 4, 5))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Checkin table';

CREATE TABLE IF NOT EXISTS `review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Review ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `review_date` DATE NOT NULL COMMENT 'Review date',
  `type` TINYINT NOT NULL COMMENT 'Type: 1 daily, 2 weekly, 3 monthly, 4 AI advice',
  `summary` TEXT DEFAULT NULL COMMENT 'Review summary',
  `ai_advice` TEXT DEFAULT NULL COMMENT 'AI advice',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_user_date_type` (`user_id`, `review_date`, `type`),
  KEY `idx_review_user_id` (`user_id`),
  KEY `idx_review_user_type` (`user_id`, `type`),
  KEY `idx_review_user_review_date` (`user_id`, `review_date`),
  CONSTRAINT `fk_review_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `ck_review_type` CHECK (`type` IN (1, 2, 3, 4))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Review table';

CREATE TABLE IF NOT EXISTS `notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Notification ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `title` VARCHAR(128) NOT NULL COMMENT 'Notification title',
  `content` TEXT NOT NULL COMMENT 'Notification content',
  `notify_time` DATETIME NOT NULL COMMENT 'Notify time',
  `channel` TINYINT NOT NULL DEFAULT 2 COMMENT 'Channel: 1 internal, 2 Feishu webhook, 3 Feishu app bot, 4 email, 5 QQ, 6 WeChat',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT 'Status: 0 pending, 1 sent, 2 failed, 3 canceled',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  KEY `idx_notification_user_id` (`user_id`),
  KEY `idx_notification_status_notify_time` (`status`, `notify_time`),
  KEY `idx_notification_user_channel` (`user_id`, `channel`),
  CONSTRAINT `fk_notification_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `ck_notification_channel` CHECK (`channel` IN (1, 2, 3, 4, 5, 6)),
  CONSTRAINT `ck_notification_status` CHECK (`status` IN (0, 1, 2, 3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Notification table';

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

CREATE TABLE IF NOT EXISTS `command_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Command log ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `feishu_message_id` VARCHAR(128) DEFAULT NULL COMMENT 'Feishu message ID for idempotency',
  `raw_text` TEXT DEFAULT NULL COMMENT 'Original command text',
  `intent` VARCHAR(32) DEFAULT NULL COMMENT 'Parsed intent',
  `task_keyword` VARCHAR(128) DEFAULT NULL COMMENT 'Parsed task keyword',
  `actual_minutes` INT DEFAULT NULL COMMENT 'Parsed actual minutes',
  `source` VARCHAR(32) DEFAULT NULL COMMENT 'Parser source: rule, dify-chat, dify-workflow, etc.',
  `success` TINYINT NOT NULL DEFAULT 0 COMMENT 'Success: 0 failed or pending, 1 success',
  `error_message` TEXT DEFAULT NULL COMMENT 'Error message',
  `reply_content` TEXT DEFAULT NULL COMMENT 'Bot reply content',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_command_log_feishu_message_id` (`feishu_message_id`),
  KEY `idx_command_log_user_created_at` (`user_id`, `created_at`),
  KEY `idx_command_log_intent` (`intent`),
  CONSTRAINT `fk_command_log_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `ck_command_log_success` CHECK (`success` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Command execution log table';

CREATE TABLE IF NOT EXISTS `conversation_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Conversation session ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `channel` VARCHAR(32) NOT NULL COMMENT 'Channel: FEISHU, QQ, WECHAT, WEB',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT 'Status: 0 active, 1 closed, 2 expired',
  `topic` VARCHAR(64) DEFAULT NULL COMMENT 'Current conversation topic',
  `state` VARCHAR(32) NOT NULL DEFAULT 'IDLE' COMMENT 'Dialogue state',
  `last_intent` VARCHAR(64) DEFAULT NULL COMMENT 'Last recognized intent',
  `expires_at` DATETIME NOT NULL COMMENT 'Session expiration time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  KEY `idx_conversation_session_user_channel` (`user_id`, `channel`, `status`, `expires_at`),
  KEY `idx_conversation_session_state` (`user_id`, `state`),
  CONSTRAINT `fk_conversation_session_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `ck_conversation_session_status` CHECK (`status` IN (0, 1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Assistant conversation session table';

CREATE TABLE IF NOT EXISTS `conversation_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Conversation message ID',
  `session_id` BIGINT NOT NULL COMMENT 'Conversation session ID',
  `user_id` BIGINT DEFAULT NULL COMMENT 'User ID',
  `channel` VARCHAR(32) NOT NULL COMMENT 'Channel: FEISHU, QQ, WECHAT, WEB',
  `direction` VARCHAR(8) NOT NULL COMMENT 'Direction: IN or OUT',
  `message_id` VARCHAR(128) DEFAULT NULL COMMENT 'External channel message ID',
  `content` TEXT DEFAULT NULL COMMENT 'Message content',
  `intent` VARCHAR(64) DEFAULT NULL COMMENT 'Recognized intent for bot reply',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_message_channel_message` (`channel`, `message_id`),
  KEY `idx_conversation_message_session` (`session_id`, `created_at`),
  KEY `idx_conversation_message_user_created` (`user_id`, `created_at`),
  CONSTRAINT `fk_conversation_message_session`
    FOREIGN KEY (`session_id`) REFERENCES `conversation_session` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_conversation_message_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
  CONSTRAINT `ck_conversation_message_direction` CHECK (`direction` IN ('IN', 'OUT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Assistant conversation message table';

CREATE TABLE IF NOT EXISTS `conversation_task_draft` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Task draft ID',
  `session_id` BIGINT DEFAULT NULL COMMENT 'Conversation session ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `title` VARCHAR(128) NOT NULL COMMENT 'Draft task title',
  `description` TEXT DEFAULT NULL COMMENT 'Draft task description',
  `plan_date` DATE DEFAULT NULL COMMENT 'Draft plan date',
  `start_time` TIME DEFAULT NULL COMMENT 'Draft start time',
  `end_time` TIME DEFAULT NULL COMMENT 'Draft end time',
  `planned_minutes` INT DEFAULT NULL COMMENT 'Draft planned minutes',
  `goal_id` BIGINT DEFAULT NULL COMMENT 'Goal ID',
  `goal_keyword` VARCHAR(128) DEFAULT NULL COMMENT 'Goal keyword from conversation',
  `missing_slots` VARCHAR(255) DEFAULT NULL COMMENT 'Missing fields to ask user',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT 'Status: 0 collecting, 1 completed, 2 cancelled, 3 expired',
  `source_text` TEXT DEFAULT NULL COMMENT 'Original or latest user text',
  `expires_at` DATETIME NOT NULL COMMENT 'Draft expiration time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  KEY `idx_conversation_task_draft_user_status` (`user_id`, `status`, `expires_at`),
  KEY `idx_conversation_task_draft_session` (`session_id`),
  CONSTRAINT `fk_conversation_task_draft_session`
    FOREIGN KEY (`session_id`) REFERENCES `conversation_session` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
  CONSTRAINT `fk_conversation_task_draft_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_conversation_task_draft_goal`
    FOREIGN KEY (`goal_id`) REFERENCES `goal` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
  CONSTRAINT `ck_conversation_task_draft_minutes` CHECK (`planned_minutes` IS NULL OR `planned_minutes` >= 0),
  CONSTRAINT `ck_conversation_task_draft_status` CHECK (`status` IN (0, 1, 2, 3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Assistant task draft table';

CREATE TABLE IF NOT EXISTS `conversation_transition_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Dialogue transition log ID',
  `session_id` BIGINT DEFAULT NULL COMMENT 'Conversation session ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `draft_id` BIGINT DEFAULT NULL COMMENT 'Task draft ID',
  `transition_type` VARCHAR(64) NOT NULL COMMENT 'Transition type',
  `raw_text` TEXT DEFAULT NULL COMMENT 'User text that triggered the transition',
  `state_before` JSON DEFAULT NULL COMMENT 'Draft state before reducer execution',
  `semantic_frame` JSON DEFAULT NULL COMMENT 'Structured frame extracted from this turn',
  `state_after` JSON DEFAULT NULL COMMENT 'Draft state after reducer execution',
  `decision` VARCHAR(32) DEFAULT NULL COMMENT 'READY, NEEDS_INPUT, CONFLICT, QUEUED, COMPLETED, or CANCELLED',
  `clarification_question` VARCHAR(512) DEFAULT NULL COMMENT 'Question returned to the user',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  PRIMARY KEY (`id`),
  KEY `idx_transition_user_created` (`user_id`, `created_at`),
  KEY `idx_transition_session_created` (`session_id`, `created_at`),
  KEY `idx_transition_draft_created` (`draft_id`, `created_at`),
  CONSTRAINT `fk_transition_session`
    FOREIGN KEY (`session_id`) REFERENCES `conversation_session` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
  CONSTRAINT `fk_transition_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_transition_draft`
    FOREIGN KEY (`draft_id`) REFERENCES `conversation_task_draft` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Deterministic dialogue state transition audit log';

INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `role`, `status`)
VALUES (1, 'local_user', '$2a$10$placeholder_password_hash', '本地用户', 'ADMIN', 1)
ON DUPLICATE KEY UPDATE `nickname` = VALUES(`nickname`), `role` = 'ADMIN', `status` = 1;
