package com.example.goalbot.agent.tool;

import com.example.goalbot.agent.ToolCall;
import com.example.goalbot.agent.ToolNames;
import com.example.goalbot.agent.ToolResult;
import com.example.goalbot.service.TaskService;
import com.example.goalbot.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ListTasksByDateTool extends AbstractAgentTool {

    private final TaskService taskService;

    @Override
    public String name() {
        return ToolNames.LIST_TASKS_BY_DATE;
    }

    @Override
    public ToolResult execute(Long userId, ToolCall call) {
        LocalDate date = dateArg(call, "plan_date");
        if (date == null) {
            date = LocalDate.now();
        }

        List<TaskVO> tasks = taskService.listActiveTasksByDate(userId, date);
        String label = dateLabel(date);
        if (tasks.isEmpty()) {
            return ToolResult.ok(label + "没有待处理任务。");
        }

        String message = tasks.stream()
                .map(task -> "- " + task.getTitle()
                        + " | " + timeRange(task)
                        + " | " + zero(task.getPlannedMinutes()) + " 分钟"
                        + " | " + statusName(task.getStatus())
                        + " | " + dash(task.getGoalTitle()))
                .collect(Collectors.joining("\n", label + "任务：\n", ""));
        return ToolResult.ok(message, tasks);
    }

    private String dateLabel(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (today.equals(date)) {
            return "今天";
        }
        if (today.plusDays(1).equals(date)) {
            return "明天";
        }
        if (today.plusDays(2).equals(date)) {
            return "后天";
        }
        if (today.minusDays(1).equals(date)) {
            return "昨天";
        }
        return date.toString();
    }

    private String timeRange(TaskVO task) {
        if (task.getStartTime() == null && task.getEndTime() == null) {
            return "未排时间";
        }
        String start = task.getStartTime() == null ? "未排时间" : task.getStartTime().toString().substring(0, 5);
        String end = task.getEndTime() == null ? null : task.getEndTime().toString().substring(0, 5);
        return end == null ? start : start + "-" + end;
    }

    private String statusName(Integer status) {
        if (Objects.equals(status, 2)) {
            return "已完成";
        }
        return "待完成";
    }
}
