package com.example.goalbot.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ToolExecutor {

    private final ToolRegistry toolRegistry;

    public ToolResult execute(Long userId, ToolCall call) {
        if (call == null || !StringUtils.hasText(call.getTool())) {
            return ToolResult.failed("我还没判断清楚要做什么，可以换个说法吗？");
        }
        return toolRegistry.find(call.getTool())
                .map(tool -> tool.execute(userId, call))
                .orElseGet(() -> ToolResult.failed("这个能力还没接上：" + call.getTool()));
    }
}
