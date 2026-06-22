package com.example.goalbot.agent.tool;

import com.example.goalbot.agent.ToolCall;
import com.example.goalbot.agent.ToolNames;
import com.example.goalbot.agent.ToolResult;
import com.example.goalbot.service.DifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdviceTool extends AbstractAgentTool {

    private final DifyService difyService;

    @Override
    public String name() {
        return ToolNames.GENERATE_ADVICE;
    }

    @Override
    public ToolResult execute(Long userId, ToolCall call) {
        return ToolResult.ok(difyService.generateAdvice(userId).getAiAdvice());
    }
}
