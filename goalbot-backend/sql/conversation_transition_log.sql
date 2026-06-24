USE goalbot;

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
