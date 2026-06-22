package com.example.goalbot.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.goalbot.dto.command.CommandIntent;
import com.example.goalbot.entity.CommandLog;
import com.example.goalbot.mapper.CommandLogMapper;
import com.example.goalbot.service.CommandLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandLogServiceImpl extends ServiceImpl<CommandLogMapper, CommandLog> implements CommandLogService {

    @Override
    public Long beginCommand(Long userId, String feishuMessageId, String rawText) {
        CommandLog commandLog = new CommandLog();
        commandLog.setUserId(userId);
        commandLog.setFeishuMessageId(StringUtils.hasText(feishuMessageId) ? feishuMessageId : null);
        commandLog.setRawText(rawText);
        commandLog.setSuccess(0);

        try {
            save(commandLog);
            return commandLog.getId();
        } catch (DuplicateKeyException ex) {
            log.info("Ignore duplicated command message. feishuMessageId={}", feishuMessageId);
            return null;
        } catch (DataAccessException ex) {
            log.warn("Command log is unavailable, command will continue without persistence: {}", ex.getMessage());
            return SKIP_LOG_ID;
        }
    }

    @Override
    public void finishCommand(Long id, CommandIntent intent, boolean success, String errorMessage, String replyContent) {
        if (id == null || SKIP_LOG_ID.equals(id)) {
            return;
        }

        CommandLog commandLog = new CommandLog();
        commandLog.setId(id);
        if (intent != null) {
            commandLog.setIntent(intent.getIntent() == null ? null : intent.getIntent().name());
            commandLog.setTaskKeyword(StringUtils.hasText(intent.getTaskKeyword())
                    ? intent.getTaskKeyword()
                    : intent.getTaskTitle());
            commandLog.setActualMinutes(intent.getActualMinutes());
            commandLog.setSource(intent.getSource());
        }
        commandLog.setSuccess(success ? 1 : 0);
        commandLog.setErrorMessage(errorMessage);
        commandLog.setReplyContent(replyContent);

        try {
            updateById(commandLog);
        } catch (DataAccessException ex) {
            log.warn("Failed to update command log. id={}, reason={}", id, ex.getMessage());
        }
    }
}
