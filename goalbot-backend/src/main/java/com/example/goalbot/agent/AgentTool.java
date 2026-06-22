package com.example.goalbot.agent;

public interface AgentTool {

    String name();

    ToolResult execute(Long userId, ToolCall call);
}
