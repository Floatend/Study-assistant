USE goalbot;

CREATE TABLE IF NOT EXISTS `agent_plan_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Agent plan log ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `session_id` BIGINT DEFAULT NULL COMMENT 'Conversation session ID',
  `message_id` VARCHAR(128) DEFAULT NULL COMMENT 'External message ID',
  `run_mode` VARCHAR(16) NOT NULL COMMENT 'OFF, SHADOW, or PRIMARY',
  `selected` TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether this plan was selected for execution',
  `plan_mode` VARCHAR(16) DEFAULT NULL COMMENT 'TOOL, CHAT, CLARIFY, or UNKNOWN',
  `confidence` DECIMAL(6,5) DEFAULT NULL COMMENT 'Planner confidence',
  `primary_tool` VARCHAR(64) DEFAULT NULL COMMENT 'First selected tool',
  `plan_json` JSON DEFAULT NULL COMMENT 'Normalized AgentPlan JSON',
  `error_message` VARCHAR(512) DEFAULT NULL COMMENT 'Planner or validation error',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  PRIMARY KEY (`id`),
  KEY `idx_agent_plan_user_created` (`user_id`, `created_at`),
  KEY `idx_agent_plan_session_created` (`session_id`, `created_at`),
  KEY `idx_agent_plan_selected_created` (`selected`, `created_at`),
  CONSTRAINT `fk_agent_plan_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_agent_plan_session`
    FOREIGN KEY (`session_id`) REFERENCES `conversation_session` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
  CONSTRAINT `ck_agent_plan_selected` CHECK (`selected` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dify AgentPlan audit log';
