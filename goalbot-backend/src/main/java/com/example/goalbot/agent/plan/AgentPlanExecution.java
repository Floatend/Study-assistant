package com.example.goalbot.agent.plan;

import com.example.goalbot.agent.ToolResult;

public record AgentPlanExecution(ToolResult result, String primaryTool) {
}
