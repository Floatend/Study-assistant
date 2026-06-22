USE goalbot;

DELETE FROM `task`
WHERE `status` IN (3, 4);

UPDATE `task`
SET `status` = 0
WHERE `status` NOT IN (0, 2);

SET @schema_name = DATABASE();

SET @sql = (
  SELECT IF(COUNT(*) > 0,
    'ALTER TABLE `task` DROP CHECK `ck_task_status`',
    'SELECT 1')
  FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'task'
    AND CONSTRAINT_NAME = 'ck_task_status'
    AND CONSTRAINT_TYPE = 'CHECK'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE `task`
  ADD CONSTRAINT `ck_task_status` CHECK (`status` IN (0, 2));
