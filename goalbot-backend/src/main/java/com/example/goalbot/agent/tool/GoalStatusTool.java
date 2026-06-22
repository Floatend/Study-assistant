package com.example.goalbot.agent.tool;

import com.example.goalbot.agent.ToolCall;
import com.example.goalbot.agent.ToolNames;
import com.example.goalbot.agent.ToolResult;
import com.example.goalbot.service.GoalService;
import com.example.goalbot.vo.GoalVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GoalStatusTool extends AbstractAgentTool {

    private final GoalService goalService;

    @Override
    public String name() {
        return ToolNames.GOAL_STATUS;
    }

    @Override
    public ToolResult execute(Long userId, ToolCall call) {
        List<GoalVO> goals = goalService.listGoals(userId, null, null, null);
        if (goals.isEmpty()) {
            return ToolResult.ok("当前还没有目标。");
        }
        String message = goals.stream()
                .map(goal -> "- " + goal.getTitle()
                        + " | " + goalStatusName(goal.getStatus())
                        + " | 任务 " + nullToZero(goal.getCompletedTaskCount()) + "/" + nullToZero(goal.getTotalTaskCount()))
                .collect(Collectors.joining("\n", "目标概览：\n", ""));
        return ToolResult.ok(message, goals);
    }

    private String goalStatusName(Integer status) {
        if (Objects.equals(status, 3)) {
            return "已完成";
        }
        if (Objects.equals(status, 2)) {
            return "已暂停";
        }
        if (Objects.equals(status, 1)) {
            return "进行中";
        }
        if (Objects.equals(status, 4)) {
            return "已归档";
        }
        return "未开始";
    }

    private long nullToZero(Long value) {
        return value == null ? 0 : value;
    }
}
