package com.example.goalbot.service;

import com.example.goalbot.dto.command.CommandIntent;

public interface FeishuCommandService {

    String handleText(Long userId, String text);

    String handleText(Long userId, String text, String feishuMessageId);

    CommandIntent parseText(Long userId, String text);
}
