package com.example.goalbot.agent;

import com.example.goalbot.dto.command.CommandIntent;

public interface AgentRuntime {

    AgentReply handle(UserMessage message);

    CommandIntent parseIntent(Long userId, String text);
}
