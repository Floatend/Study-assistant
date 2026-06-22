package com.example.goalbot.service.impl;

import com.example.goalbot.agent.AgentReply;
import com.example.goalbot.agent.AgentRuntime;
import com.example.goalbot.agent.UserMessage;
import com.example.goalbot.dto.command.CommandIntent;
import com.example.goalbot.service.FeishuCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeishuCommandServiceImpl implements FeishuCommandService {

    private static final String CHANNEL_FEISHU = "FEISHU";

    private final AgentRuntime agentRuntime;

    @Override
    public String handleText(Long userId, String text) {
        return handleText(userId, text, null);
    }

    @Override
    public String handleText(Long userId, String text, String feishuMessageId) {
        UserMessage message = new UserMessage();
        message.setUserId(userId);
        message.setChannel(CHANNEL_FEISHU);
        message.setMessageId(feishuMessageId);
        message.setText(text);

        AgentReply reply = agentRuntime.handle(message);
        return reply == null ? null : reply.getContent();
    }

    @Override
    public CommandIntent parseText(Long userId, String text) {
        return agentRuntime.parseIntent(userId, text);
    }
}
