package com.example.goalbot.agent.tool;

import com.example.goalbot.agent.ToolCall;
import com.example.goalbot.agent.ToolNames;
import com.example.goalbot.agent.ToolResult;
import com.example.goalbot.service.TaskService;
import com.example.goalbot.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ListTodayTasksTool extends AbstractAgentTool {

    private final TaskService taskService;

    @Override
    public String name() {
        return ToolNames.LIST_TODAY_TASKS;
    }

    @Override
    public ToolResult execute(Long userId, ToolCall call) {
        List<TaskVO> tasks = taskService.listTodayTasks(userId);
        if (tasks.isEmpty()) {
            return ToolResult.ok("今天没有任务。");
        }
        String message = tasks.stream()
                .map(task -> "- " + task.getTitle()
                        + " | " + zero(task.getPlannedMinutes()) + " 分钟"
                        + " | " + statusName(task.getStatus())
                        + " | " + dash(task.getGoalTitle()))
                .collect(Collectors.joining("\n", "今日任务：\n", ""));
        return ToolResult.ok(message, tasks);
    }

    private String statusName(Integer status) {
        if (Objects.equals(status, 2)) {
            return "已完成";
        }
        return "待完成";
    }
}
