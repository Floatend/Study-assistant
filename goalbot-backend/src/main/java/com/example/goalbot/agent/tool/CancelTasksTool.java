package com.example.goalbot.agent.tool;

import com.example.goalbot.agent.ToolCall;
import com.example.goalbot.agent.ToolNames;
import com.example.goalbot.agent.ToolResult;
import com.example.goalbot.service.TaskService;
import com.example.goalbot.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CancelTasksTool extends AbstractAgentTool {

    private final TaskService taskService;

    @Override
    public String name() {
        return ToolNames.CANCEL_TASKS;
    }

    @Override
    public ToolResult execute(Long userId, ToolCall call) {
        LocalDate date = dateArg(call, "plan_date");
        LocalDate rangeStartDate = dateArg(call, "range_start_date");
        LocalDate rangeEndDate = dateArg(call, "range_end_date");
        String keyword = stringArg(call, "task_keyword");

        if ((rangeStartDate == null) != (rangeEndDate == null)) {
            return ToolResult.ok("删除范围不完整，我没有删除任何任务。请补充开始和结束日期。");
        }
        if (rangeStartDate != null) {
            return deleteRange(userId, rangeStartDate, rangeEndDate, keyword);
        }
        if (date == null) {
            return ToolResult.ok("我知道你想删除任务，但还没有确定日期，所以没有删除任何内容。"
                    + "你可以说“删除明天所有任务”或“把下下周的任务删掉”。");
        }

        List<TaskVO> deletedTasks = StringUtils.hasText(keyword)
                ? taskService.deletePendingTasksInRange(userId, date, date, keyword)
                : taskService.cancelTasksByDate(userId, date);
        if (deletedTasks.isEmpty()) {
            return ToolResult.ok(dateLabel(date) + "没有找到"
                    + (StringUtils.hasText(keyword) ? "与“" + keyword + "”匹配的" : "")
                    + "未完成任务。");
        }

        String taskLines = deletedTasks.stream()
                .map(task -> "- " + task.getTitle())
                .collect(Collectors.joining("\n"));
        return ToolResult.ok("已删除" + dateLabel(date) + " " + deletedTasks.size() + " 个未完成任务：\n" + taskLines,
                deletedTasks);
    }

    private ToolResult deleteRange(Long userId, LocalDate startDate, LocalDate endDate, String keyword) {
        List<TaskVO> deletedTasks = taskService.deletePendingTasksInRange(userId, startDate, endDate, keyword);
        String rangeLabel = startDate + " 至 " + endDate;
        String keywordLabel = StringUtils.hasText(keyword) ? "与“" + keyword + "”匹配的" : "";
        if (deletedTasks.isEmpty()) {
            return ToolResult.ok(rangeLabel + " 没有找到" + keywordLabel + "未完成任务。");
        }

        String taskLines = deletedTasks.stream()
                .map(task -> "- " + task.getPlanDate() + " " + task.getTitle())
                .collect(Collectors.joining("\n"));
        return ToolResult.ok("已删除 " + rangeLabel + " 的 " + deletedTasks.size() + " 个"
                        + keywordLabel + "未完成任务：\n" + taskLines,
                deletedTasks);
    }

    private String dateLabel(LocalDate date) {
        if (LocalDate.now().equals(date)) {
            return "今天";
        }
        if (LocalDate.now().plusDays(1).equals(date)) {
            return "明天";
        }
        return date.toString();
    }
}
