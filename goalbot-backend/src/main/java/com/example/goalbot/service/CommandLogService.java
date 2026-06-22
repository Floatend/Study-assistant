package com.example.goalbot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.goalbot.dto.command.CommandIntent;
import com.example.goalbot.entity.CommandLog;

public interface CommandLogService extends IService<CommandLog> {

    Long SKIP_LOG_ID = -1L;

    Long beginCommand(Long userId, String feishuMessageId, String rawText);

    void finishCommand(Long id, CommandIntent intent, boolean success, String errorMessage, String replyContent);
}
