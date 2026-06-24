USE goalbot;

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
